package com.passport.photo.service;

import com.passport.photo.model.AnalysisResult;
import com.passport.photo.model.ComplianceCheck;
import com.passport.photo.model.CountrySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the face-ratio compliance check and face-aware correction path
 * introduced in {@link PhotoAnalysisService}.
 *
 * <p>{@link FaceDetectionService} is mocked so no OpenCV native libraries need
 * to be loaded. All image data is generated in-memory using small synthetic
 * {@link BufferedImage} objects — no real photos are required.</p>
 *
 * <h2>Check-index convention (analyzePhoto returns 5 checks)</h2>
 * <ol start="0">
 *   <li>Image Dimensions</li>
 *   <li>Aspect Ratio</li>
 *   <li>Background Color</li>
 *   <li>Resolution / Quality</li>
 *   <li>Face Size / Face Presence (estimated) ← tested here</li>
 * </ol>
 */
class PhotoAnalysisServiceFaceRatioTest {

    /** Mocked FaceDetectionService — no native OpenCV loading. */
    private FaceDetectionService mockFds;

    /** Service under test wired with the mock. */
    private PhotoAnalysisService serviceWithFds;

    /**
     * Service under test with {@code null} FaceDetectionService — exercises all
     * fall-back paths that are triggered when no detector is available.
     */
    private PhotoAnalysisService serviceNoFds;

    // ─── setup ───────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        mockFds = mock(FaceDetectionService.class);
        serviceWithFds = new PhotoAnalysisService(mockFds);
        serviceNoFds   = new PhotoAnalysisService();   // null FDS — uses heuristic
    }

    // ─── image factories ─────────────────────────────────────────────────────────

    private byte[] toBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    /**
     * All-white image — passes background check, fails face-position heuristic
     * (uniform, low variance).
     */
    private BufferedImage whiteImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    /**
     * White corners + alternating stripes in the upper-centre — passes the
     * colour-variance heuristic (variance > 200).
     */
    private BufferedImage variedCenterWhiteImage(int w, int h) {
        BufferedImage img = whiteImage(w, h);
        Graphics2D g = img.createGraphics();
        int x0 = w / 4, x1 = 3 * w / 4;
        int y0 = h / 8, y1 = 5 * h / 8;
        for (int y = y0; y < y1; y++) {
            g.setColor(y % 4 < 2 ? Color.BLACK : new Color(50, 80, 120));
            g.drawLine(x0, y, x1, y);
        }
        g.dispose();
        return img;
    }

    /**
     * Builds a spec with the given face-ratio bounds. Dimensions are set to
     * US values (600×600 px) so that dimensions/aspect-ratio/resolution checks
     * also pass when the image is 600×600.
     */
    private CountrySpec specWithRatios(double min, double max) {
        CountrySpec spec = new CountrySpec();
        spec.setName("TestCountry");
        spec.setWidthPx(600);
        spec.setHeightPx(600);
        spec.setWidthMm(51);
        spec.setHeightMm(51);
        spec.setDpi(300);
        spec.setBackgroundColorHex("#FFFFFF");
        spec.setFaceRatioMin(min);
        spec.setFaceRatioMax(max);
        return spec;
    }

    /**
     * Returns check[4] (the face check) from an {@code analyzePhoto} call
     * against the supplied spec.
     */
    private ComplianceCheck faceCheck(PhotoAnalysisService svc, byte[] imageBytes, CountrySpec spec)
            throws IOException {
        AnalysisResult result = svc.analyzePhoto(imageBytes, spec);
        return result.getChecks().get(4);
    }

    // ─── fallback to heuristic when FDS unavailable ───────────────────────────────

    @Test
    void checkFaceRatio_fallsBackToHeuristic_whenFdsIsNull() throws IOException {
        // serviceNoFds has null FDS — must not throw NPE; uses heuristic instead
        byte[] imageBytes = toBytes(variedCenterWhiteImage(600, 600));
        CountrySpec spec = specWithRatios(0.50, 0.69);

        ComplianceCheck check = faceCheck(serviceNoFds, imageBytes, spec);

        // The heuristic names the check "Face Presence (estimated)"
        assertThat(check.getRule()).isEqualTo("Face Presence (estimated)");
        assertThat(check.isPassed()).isTrue();   // varied-centre image passes heuristic
    }

    @Test
    void checkFaceRatio_fallsBackToHeuristic_whenFaceRatioMinIsZero() throws IOException {
        // faceRatioMin == 0 → condition is false → heuristic path, FDS never called
        byte[] imageBytes = toBytes(variedCenterWhiteImage(600, 600));
        CountrySpec spec = specWithRatios(0.0, 0.69);

        ComplianceCheck check = faceCheck(serviceWithFds, imageBytes, spec);

        assertThat(check.getRule()).isEqualTo("Face Presence (estimated)");
        verifyNoInteractions(mockFds);
    }

    @Test
    void checkFaceRatio_fallsBackToHeuristic_whenFaceRatioMaxIsZero() throws IOException {
        // faceRatioMax == 0 → condition is false → heuristic path, FDS never called
        byte[] imageBytes = toBytes(variedCenterWhiteImage(600, 600));
        CountrySpec spec = specWithRatios(0.50, 0.0);

        ComplianceCheck check = faceCheck(serviceWithFds, imageBytes, spec);

        assertThat(check.getRule()).isEqualTo("Face Presence (estimated)");
        verifyNoInteractions(mockFds);
    }

    @Test
    void checkFaceRatio_fallsBackToHeuristic_whenDetectPrimaryFaceReturnsNull() throws IOException {
        // FDS returns null (no face found) → falls back to heuristic
        when(mockFds.detectPrimaryFace(any())).thenReturn(null);

        byte[] imageBytes = toBytes(variedCenterWhiteImage(600, 600));
        CountrySpec spec = specWithRatios(0.50, 0.69);

        ComplianceCheck check = faceCheck(serviceWithFds, imageBytes, spec);

        assertThat(check.getRule()).isEqualTo("Face Presence (estimated)");
    }

    @Test
    void checkFaceRatio_fallsBackToHeuristic_whenDetectPrimaryFaceThrows() throws IOException {
        // FDS throws an exception → must not propagate; falls back to heuristic
        when(mockFds.detectPrimaryFace(any()))
                .thenThrow(new IllegalArgumentException("Cannot decode image"));

        byte[] imageBytes = toBytes(variedCenterWhiteImage(600, 600));
        CountrySpec spec = specWithRatios(0.50, 0.69);

        // Must not throw
        ComplianceCheck check = faceCheck(serviceWithFds, imageBytes, spec);

        assertThat(check.getRule()).isEqualTo("Face Presence (estimated)");
    }

    // ─── Face Size check passes and fails ─────────────────────────────────────────

    @Test
    void checkFaceRatio_passes_whenFaceHeightIsWithinRange() throws IOException {
        // Image: 600×600. Face height = 330 px → faceRatio = 0.55.
        // Spec range [0.50, 0.69] → should PASS.
        int imageH = 600;
        int faceH  = 330;   // 330/600 = 0.55 — within [0.50, 0.69]
        when(mockFds.detectPrimaryFace(any()))
                .thenReturn(new int[]{100, 80, 180, faceH});

        byte[] imageBytes = toBytes(whiteImage(600, imageH));
        CountrySpec spec = specWithRatios(0.50, 0.69);

        ComplianceCheck check = faceCheck(serviceWithFds, imageBytes, spec);

        assertThat(check.getRule()).isEqualTo("Face Size");
        assertThat(check.isPassed()).isTrue();
        assertThat(check.getMessage()).contains("within the required range");
    }

    @Test
    void checkFaceRatio_fails_withFaceTooSmall_whenBelowMin() throws IOException {
        // Image: 600×600. Face height = 180 px → faceRatio = 0.30.
        // Spec min = 0.50 → FAIL "Face too small".
        int imageH = 600;
        int faceH  = 180;   // 180/600 = 0.30 — below min 0.50
        when(mockFds.detectPrimaryFace(any()))
                .thenReturn(new int[]{150, 120, 180, faceH});

        byte[] imageBytes = toBytes(whiteImage(600, imageH));
        CountrySpec spec = specWithRatios(0.50, 0.69);

        ComplianceCheck check = faceCheck(serviceWithFds, imageBytes, spec);

        assertThat(check.getRule()).isEqualTo("Face Size");
        assertThat(check.isPassed()).isFalse();
        assertThat(check.getMessage()).contains("Face too small");
    }

    @Test
    void checkFaceRatio_fails_withFaceTooLarge_whenAboveMax() throws IOException {
        // Image: 600×600. Face height = 480 px → faceRatio = 0.80.
        // Spec max = 0.69 → FAIL "Face too large".
        int imageH = 600;
        int faceH  = 480;   // 480/600 = 0.80 — above max 0.69
        when(mockFds.detectPrimaryFace(any()))
                .thenReturn(new int[]{60, 30, 400, faceH});

        byte[] imageBytes = toBytes(whiteImage(600, imageH));
        CountrySpec spec = specWithRatios(0.50, 0.69);

        ComplianceCheck check = faceCheck(serviceWithFds, imageBytes, spec);

        assertThat(check.getRule()).isEqualTo("Face Size");
        assertThat(check.isPassed()).isFalse();
        assertThat(check.getMessage()).contains("Face too large");
    }

    // ─── boundary conditions ──────────────────────────────────────────────────────

    @Test
    void checkFaceRatio_passes_atExactFaceRatioMin() throws IOException {
        // faceRatio == faceRatioMin → passes (condition uses >=)
        // Image: 600×600. faceRatioMin = 0.50. Face height = 300 px → ratio = 0.50 exactly.
        int imageH = 600;
        int faceH  = 300;   // 300/600 = 0.50 exactly
        when(mockFds.detectPrimaryFace(any()))
                .thenReturn(new int[]{150, 50, 200, faceH});

        byte[] imageBytes = toBytes(whiteImage(600, imageH));
        CountrySpec spec = specWithRatios(0.50, 0.69);

        ComplianceCheck check = faceCheck(serviceWithFds, imageBytes, spec);

        assertThat(check.getRule()).isEqualTo("Face Size");
        assertThat(check.isPassed()).isTrue();
    }

    @Test
    void checkFaceRatio_passes_atExactFaceRatioMax() throws IOException {
        // faceRatio == faceRatioMax → passes (condition uses <=)
        // Image: 600×600. faceRatioMax = 0.69. Closest integer: faceH = 414 → 414/600 = 0.69.
        int imageH = 600;
        int faceH  = 414;   // 414/600 = 0.69 exactly
        when(mockFds.detectPrimaryFace(any()))
                .thenReturn(new int[]{93, 20, 250, faceH});

        byte[] imageBytes = toBytes(whiteImage(600, imageH));
        CountrySpec spec = specWithRatios(0.50, 0.69);

        ComplianceCheck check = faceCheck(serviceWithFds, imageBytes, spec);

        assertThat(check.getRule()).isEqualTo("Face Size");
        assertThat(check.isPassed()).isTrue();
    }

    // ─── correctPhoto — face-aware crop path ─────────────────────────────────────

    @Test
    void correctPhoto_faceAwarePath_returnsExactTargetDimensions() throws IOException {
        // Source: 800×1000. Face at (200, 100, 300, 400) → faceH/srcH = 0.40.
        // Spec: 600×600, ratios [0.50, 0.69].
        // targetRatio = 0.69 → cropH = 400/0.69 ≈ 580 px; cropW ≈ 580 px.
        // Both fit within 800×1000, so face-aware path is taken.
        when(mockFds.detectPrimaryFace(any()))
                .thenReturn(new int[]{200, 100, 300, 400});

        BufferedImage src = whiteImage(800, 1000);
        byte[] imageBytes = toBytes(src);

        CountrySpec spec = specWithRatios(0.50, 0.69);
        byte[] corrected = serviceWithFds.correctPhoto(imageBytes, spec);

        BufferedImage out = ImageIO.read(new ByteArrayInputStream(corrected));
        assertThat(out).isNotNull();
        assertThat(out.getWidth()).isEqualTo(600);
        assertThat(out.getHeight()).isEqualTo(600);
    }

    @Test
    void correctPhoto_fallsBackToCoverMode_whenCropExceedsSourceSize() throws IOException {
        // Source: 100×100 (tiny image). Face bounding box deliberately placed so that the
        // computed cropH = faceH / targetRatio would exceed the source height, forcing
        // the face-aware condition (cropW <= src.getWidth() && cropH <= src.getHeight()) to
        // be false → falls back to cover-mode.
        // Face height = 90 px on a 100×100 image → targetRatio=0.69 → cropH ≈ 130 px > 100.
        when(mockFds.detectPrimaryFace(any()))
                .thenReturn(new int[]{5, 5, 80, 90});

        BufferedImage src = whiteImage(100, 100);
        byte[] imageBytes = toBytes(src);

        CountrySpec spec = specWithRatios(0.50, 0.69);
        byte[] corrected = serviceWithFds.correctPhoto(imageBytes, spec);

        BufferedImage out = ImageIO.read(new ByteArrayInputStream(corrected));
        assertThat(out).isNotNull();
        // Output must ALWAYS be exactly target dimensions regardless of path taken
        assertThat(out.getWidth()).isEqualTo(600);
        assertThat(out.getHeight()).isEqualTo(600);
    }

    @Test
    void correctPhoto_fallsBackToCoverMode_whenFaceRectIsNull() throws IOException {
        // FDS returns null (no face found) → cover-mode path, output still exact spec size
        when(mockFds.detectPrimaryFace(any())).thenReturn(null);

        byte[] imageBytes = toBytes(whiteImage(800, 1000));
        CountrySpec spec = specWithRatios(0.50, 0.69);

        byte[] corrected = serviceWithFds.correctPhoto(imageBytes, spec);

        BufferedImage out = ImageIO.read(new ByteArrayInputStream(corrected));
        assertThat(out).isNotNull();
        assertThat(out.getWidth()).isEqualTo(600);
        assertThat(out.getHeight()).isEqualTo(600);
    }
}
