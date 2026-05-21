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

@Service
public class FaceDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FaceDetectionService.class);

    private CascadeClassifier classifier;

    @PostConstruct
    public void init() throws IOException {
        // Load native OpenCV libraries for the current platform
        Loader.load(CascadeClassifier.class);

        ClassPathResource cascadeResource = new ClassPathResource("haarcascade_frontalface_default.xml");
        if (!cascadeResource.exists()) {
            throw new IllegalStateException(
                "Haar cascade XML not found. Run: mvn generate-resources spring-boot:run\n" +
                "Maven downloads it automatically on the first build."
            );
        }

        // CascadeClassifier requires a real filesystem path, not an InputStream
        File tempCascade = File.createTempFile("haarcascade_frontalface", ".xml");
        tempCascade.deleteOnExit();
        try (InputStream is = cascadeResource.getInputStream()) {
            Files.copy(is, tempCascade.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        classifier = new CascadeClassifier(tempCascade.getAbsolutePath());
        if (classifier.empty()) {
            throw new IllegalStateException("CascadeClassifier loaded but is empty — XML may be corrupt.");
        }

        log.info("Haar Cascade face detector ready.");
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

        RectVector faces = new RectVector();
        classifier.detectMultiScale(
            gray,
            faces,
            1.1,              // scale factor per pyramid step
            3,                // min neighbours — 3 balances sensitivity vs false positives
            0,                // flags (unused in modern OpenCV)
            new Size(30, 30), // minimum face size — small so distant/small faces still pass
            new Size()        // maximum face size — no limit
        );

        int count = (int) faces.size();
        gray.close();
        faces.close();
        return count;
    }
}
