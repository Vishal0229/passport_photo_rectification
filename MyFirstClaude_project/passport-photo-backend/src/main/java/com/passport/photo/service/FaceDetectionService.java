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

    // Paths to the extracted cascade XML files — classifiers are created per-call for thread safety.
    private String defaultCascadePath;
    private String alt2CascadePath;

    /**
     * Extracts both Haar Cascade XML files to temp paths and verifies they load cleanly.
     * Called automatically by Spring after bean construction.
     *
     * @throws IOException           if a cascade XML cannot be copied to a temp file
     * @throws IllegalStateException if a cascade file is missing from the classpath or is empty
     */
    @PostConstruct
    public void init() throws IOException {
        Loader.load(CascadeClassifier.class);
        defaultCascadePath = extractCascade("haarcascade_frontalface_default.xml");
        alt2CascadePath    = extractCascade("haarcascade_frontalface_alt2.xml");
        verifyCascade(defaultCascadePath, "haarcascade_frontalface_default.xml");
        verifyCascade(alt2CascadePath,    "haarcascade_frontalface_alt2.xml");
        log.info("Haar Cascade face detectors ready (default + alt2).");
    }

    private String extractCascade(String resourceName) throws IOException {
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
        return tmp.getAbsolutePath();
    }

    private void verifyCascade(String path, String name) {
        try (CascadeClassifier cc = new CascadeClassifier(path)) {
            if (cc.empty()) throw new IllegalStateException(name + " loaded but is empty.");
        }
    }

    /**
     * Returns the number of faces detected in the supplied raw image bytes.
     *
     * <p>A new {@link CascadeClassifier} is created per call so concurrent requests
     * never share mutable native state (CascadeClassifier is not thread-safe).
     * All native OpenCV resources are released via try-with-resources in all paths.</p>
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

        try {
            // First pass: strict (minNeighbors=4) — low false-positive risk
            int count;
            try (CascadeClassifier cc = new CascadeClassifier(defaultCascadePath);
                 RectVector faces     = new RectVector()) {
                cc.detectMultiScale(gray, faces, 1.1, 4, 0, new Size(30, 30), new Size());
                count = (int) faces.size();
            }

            if (count == 0) {
                // Second pass: alt2 cascade + upscale 2× + equalise.
                // Catches faces that are small, angled, or in scanned prints.
                // Safe because the controller blocks count > 1.
                try (Mat up                  = new Mat();
                     CascadeClassifier cc2   = new CascadeClassifier(alt2CascadePath);
                     RectVector faces2       = new RectVector()) {
                    resize(gray, up, new Size(gray.cols() * 2, gray.rows() * 2), 0, 0, INTER_LINEAR);
                    equalizeHist(up, up);
                    cc2.detectMultiScale(up, faces2, 1.05, 2, 0, new Size(30, 30), new Size());
                    count = (int) faces2.size();
                }
            }
            return count;
        } finally {
            gray.close();
        }
    }
}
