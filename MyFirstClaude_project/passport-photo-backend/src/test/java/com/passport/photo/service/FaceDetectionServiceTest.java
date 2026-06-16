package com.passport.photo.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for {@link FaceDetectionService#detectPrimaryFace}.
 *
 * <p>Uses {@code @SpringBootTest} so that the Haar Cascade classifiers are
 * fully initialised via the {@code @PostConstruct init()} method (OpenCV
 * requires real filesystem paths to the cascade XML files, which Spring loads
 * from the classpath into temp files at startup).</p>
 */
@SpringBootTest
class FaceDetectionServiceTest {

    @Autowired
    private FaceDetectionService faceDetectionService;

    // ─── helpers ─────────────────────────────────────────────────────────────────

    private byte[] toBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    /** Solid single-colour image — no face structure whatsoever. */
    private BufferedImage solidColorImage(int w, int h, Color color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    // ─── invalid / empty input ────────────────────────────────────────────────────

    @Test
    void detectPrimaryFace_throwsIAE_forNonImageBytes() {
        byte[] garbage = "this-is-not-an-image".getBytes();
        assertThatThrownBy(() -> faceDetectionService.detectPrimaryFace(garbage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot decode image");
    }

    @Test
    void detectPrimaryFace_throwsIAE_forEmptyByteArray() {
        byte[] empty = new byte[0];
        // An empty byte array cannot be decoded as an image by OpenCV's imdecode
        assertThatThrownBy(() -> faceDetectionService.detectPrimaryFace(empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot decode image");
    }

    // ─── solid-colour image (no face structure) ───────────────────────────────────

    @Test
    void detectPrimaryFace_returnsNull_forSolidWhiteImage() throws IOException {
        // A uniform solid-colour image has no face-like structure; both Haar Cascade
        // passes will find zero detections and the method should return null.
        byte[] solidWhite = toBytes(solidColorImage(200, 200, Color.WHITE));
        int[] result = faceDetectionService.detectPrimaryFace(solidWhite);
        assertThat(result).isNull();
    }
}
