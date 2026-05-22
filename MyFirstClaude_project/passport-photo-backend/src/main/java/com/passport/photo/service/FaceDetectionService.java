package com.passport.photo.service;

import jakarta.annotation.PostConstruct;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgproc.equalizeHist;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_LINEAR;

/**
 * Detects human faces in images using OpenCV Haar Cascade classifiers.
 *
 * <p>Uses a two-pass strategy to balance accuracy and false-positive rate:
 * <ol>
 *   <li><strong>Pass 1</strong> — {@code haarcascade_frontalface_default.xml} with strict
 *       parameters (minNeighbors=4). Low false-positive risk; may miss small or angled faces.</li>
 *   <li><strong>Pass 2</strong> (only when pass 1 finds zero faces) —
 *       {@code haarcascade_frontalface_alt2.xml} with the image upscaled 2× and histogram
 *       equalisation applied. More pose-tolerant; catches faces in scanned prints and low-res
 *       photos. Using pass 2 only when count is already 0 keeps the false-positive rate safe
 *       because the controller blocks {@code count > 1}.</li>
 * </ol>
 * </p>
 *
 * <p>The cascade XML files are loaded from the classpath at startup. The default cascade is
 * downloaded at Maven build time via {@code download-maven-plugin}; the alt2 cascade is
 * bundled directly in {@code src/main/resources/}.</p>
 *
 * @version 1.0
 */
@Service
public class FaceDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FaceDetectionService.class);

    private CascadeClassifier classifier;
    private CascadeClassifier classifierAlt2;

    /**
     * Loads both Haar Cascade classifiers from the classpath into temporary files.
     * Called automatically by Spring after bean construction.
     *
     * @throws IOException           if the cascade XML resource cannot be copied to a temp file
     * @throws IllegalStateException if a cascade file is missing from the classpath or is empty
     */
    @PostConstruct
    public void init() throws IOException {
        Loader.load(CascadeClassifier.class);
        classifier    = loadCascade("haarcascade_frontalface_default.xml");
        classifierAlt2 = loadCascade("haarcascade_frontalface_alt2.xml");
        log.info("Haar Cascade face detectors ready (default + alt2).");
    }

    /**
     * Copies a classpath cascade XML resource to a temporary file and loads it.
     *
     * <p>OpenCV requires a filesystem path — it cannot read directly from a JAR stream.
     * The temporary file is marked for deletion on JVM exit.</p>
     *
     * @param resourceName classpath resource name (e.g. {@code "haarcascade_frontalface_default.xml"})
     * @return a loaded, non-empty {@link CascadeClassifier}
     * @throws IOException           if the temp file cannot be created or written
     * @throws IllegalStateException if the resource is not found or the classifier is empty after loading
     */
    private CascadeClassifier loadCascade(String resourceName) throws IOException {
        ClassPathResource res = new ClassPathResource(resourceName);
        if (!res.exists()) {
            throw new IllegalStateException(resourceName + " not found on classpath. " +
                "Run: mvn generate-resources spring-boot:run");
        }
        File tmp = File.createTempFile("cascade_", ".xml");
        tmp.deleteOnExit();
        try (InputStream is = res.getInputStream()) {
            Files.copy(is, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        CascadeClassifier cc = new CascadeClassifier(tmp.getAbsolutePath());
        if (cc.empty()) throw new IllegalStateException(resourceName + " loaded but is empty.");
        return cc;
    }

    /**
     * Returns the number of faces detected in the supplied raw image bytes.
     * Throws IllegalArgumentException if the bytes cannot be decoded as an image.
     */
    public int countFaces(byte[] imageBytes) {
        BytePointer bp = new BytePointer(imageBytes);
        Mat buffer = new Mat(1, imageBytes.length, CV_8UC1, bp);
        Mat gray = imdecode(buffer, IMREAD_GRAYSCALE);
        bp.close();
        buffer.close();

        if (gray.empty()) {
            gray.close();
            throw new IllegalArgumentException("Cannot decode image — please upload a valid JPEG or PNG.");
        }

        // First pass: strict (minNeighbors=4) — low false-positive risk
        RectVector faces = new RectVector();
        classifier.detectMultiScale(gray, faces, 1.1, 4, 0, new Size(30, 30), new Size());
        int count = (int) faces.size();
        faces.close();

        if (count == 0) {
            // Second pass: alt2 cascade (more pose-tolerant) + upscale 2× + equalise.
            // Catches faces that are small, angled, or in scanned prints.
            // Safe because the controller blocks count > 1.
            Mat up = new Mat();
            resize(gray, up, new Size(gray.cols() * 2, gray.rows() * 2), 0, 0, INTER_LINEAR);
            equalizeHist(up, up);
            RectVector faces2 = new RectVector();
            classifierAlt2.detectMultiScale(up, faces2, 1.05, 2, 0, new Size(30, 30), new Size());
            count = (int) faces2.size();
            faces2.close();
            up.close();
        }

        gray.close();
        return count;
    }
}
