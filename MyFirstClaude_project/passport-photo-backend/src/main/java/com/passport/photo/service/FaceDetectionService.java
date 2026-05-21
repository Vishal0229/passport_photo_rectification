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

@Service
public class FaceDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FaceDetectionService.class);

    private CascadeClassifier classifier;
    private CascadeClassifier classifierAlt2;

    @PostConstruct
    public void init() throws IOException {
        Loader.load(CascadeClassifier.class);
        classifier    = loadCascade("haarcascade_frontalface_default.xml");
        classifierAlt2 = loadCascade("haarcascade_frontalface_alt2.xml");
        log.info("Haar Cascade face detectors ready (default + alt2).");
    }

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
