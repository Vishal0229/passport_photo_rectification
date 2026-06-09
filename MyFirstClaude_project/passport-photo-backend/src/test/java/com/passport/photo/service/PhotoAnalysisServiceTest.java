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
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class PhotoAnalysisServiceTest {

    private PhotoAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new PhotoAnalysisService();
    }

    // ─── image factories ─────────────────────────────────────────────────────────

    private byte[] toBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    /** All-white image. Passes background check; fails face-position (uniform). */
    private BufferedImage whiteImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    /** All-black image. Fails background check; fails face-position (uniform). */
    private BufferedImage darkImage(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * White corners (passes background) + alternating black/coloured stripes
     * in the upper-centre region (passes face-position variance > 200).
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

    // ─── getCountrySpecs ─────────────────────────────────────────────────────────

    @Test
    void getCountrySpecs_returnsAllFifteenCountries() {
        Map<String, CountrySpec> specs = service.getCountrySpecs();
        assertThat(specs).hasSize(15)
            .containsKeys(
                "US", "UK", "India", "Canada", "Australia", "UAE", "Schengen",
                "Germany", "France", "Japan", "China", "Brazil", "NewZealand", "Singapore", "Mexico"
            );
    }

    @Test
    void getCountrySpecs_allCountriesHaveRequirementsBulletPoints() {
        Map<String, CountrySpec> specs = service.getCountrySpecs();
        for (Map.Entry<String, CountrySpec> entry : specs.entrySet()) {
            CountrySpec spec = entry.getValue();
            assertThat(spec.getRequirementsBulletPoints())
                .as("requirementsBulletPoints for country '%s' should not be null", entry.getKey())
                .isNotNull();
            assertThat(spec.getRequirementsBulletPoints())
                .as("requirementsBulletPoints for country '%s' should not be empty", entry.getKey())
                .isNotEmpty();
        }
    }

    @Test
    void usSpec_hasCorrectDimensions() {
        CountrySpec us = service.getCountrySpecs().get("US");
        assertThat(us.getWidthPx()).isEqualTo(600);
        assertThat(us.getHeightPx()).isEqualTo(600);
        assertThat(us.getDpi()).isEqualTo(300);
        assertThat(us.getBackgroundColorHex()).isEqualTo("#FFFFFF");
    }

    // ─── analyzePhoto — country string ───────────────────────────────────────────

    @Test
    void analyzePhoto_withValidCountry_returnsFiveChecks() throws IOException {
        AnalysisResult result = service.analyzePhoto(toBytes(variedCenterWhiteImage(600, 600)), "US");
        assertThat(result.getTotalCount()).isEqualTo(5);
        assertThat(result.getChecks()).hasSize(5);
        // country field stores the lookup key, not the spec's display name
        assertThat(result.getCountry()).isEqualTo("US");
    }

    @Test
    void analyzePhoto_withUnknownCountry_throwsIllegalArgumentException() throws IOException {
        byte[] imageBytes = toBytes(whiteImage(100, 100));
        assertThatThrownBy(() -> service.analyzePhoto(imageBytes, "XYZ"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown country code");
    }

    @Test
    void analyzePhoto_withInvalidImageBytes_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.analyzePhoto("not-an-image".getBytes(), "US"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot read image");
    }

    @Test
    void analyzePhoto_withCustomSpec_usesSpecName() throws IOException {
        CountrySpec spec = new CountrySpec();
        spec.setName("Andorra");
        spec.setWidthPx(400); spec.setHeightPx(500);
        spec.setDpi(300); spec.setBackgroundColorHex("#FFFFFF");

        AnalysisResult result = service.analyzePhoto(toBytes(whiteImage(400, 500)), spec);
        assertThat(result.getCountry()).isEqualTo("Andorra");
    }

    @Test
    void analyzePhoto_withCustomSpecBlankName_usesCustomLabel() throws IOException {
        CountrySpec spec = new CountrySpec();
        spec.setName("   ");
        spec.setWidthPx(400); spec.setHeightPx(500);
        spec.setDpi(300); spec.setBackgroundColorHex("#FFFFFF");

        AnalysisResult result = service.analyzePhoto(toBytes(whiteImage(400, 500)), spec);
        assertThat(result.getCountry()).isEqualTo("Custom");
    }

    // ─── check[0]: Image Dimensions ──────────────────────────────────────────────

    @Test
    void checkDimensions_passes_whenImageMeetsMinimumSize() throws IOException {
        AnalysisResult result = service.analyzePhoto(toBytes(whiteImage(600, 600)), "US");
        ComplianceCheck check = result.getChecks().get(0);
        assertThat(check.getRule()).isEqualTo("Image Dimensions");
        assertThat(check.isPassed()).isTrue();
    }

    @Test
    void checkDimensions_fails_whenImageIsSmallerThanSpec() throws IOException {
        AnalysisResult result = service.analyzePhoto(toBytes(whiteImage(100, 100)), "US");
        ComplianceCheck check = result.getChecks().get(0);
        assertThat(check.isPassed()).isFalse();
        assertThat(check.getMessage()).contains("smaller than required");
    }

    // ─── check[1]: Aspect Ratio ──────────────────────────────────────────────────

    @Test
    void checkAspectRatio_passes_withMatchingSquareImageForUSSpec() throws IOException {
        AnalysisResult result = service.analyzePhoto(toBytes(whiteImage(600, 600)), "US");
        ComplianceCheck check = result.getChecks().get(1);
        assertThat(check.getRule()).isEqualTo("Aspect Ratio");
        assertThat(check.isPassed()).isTrue();
    }

    @Test
    void checkAspectRatio_fails_withExtremelyWideImage() throws IOException {
        // 1200×200 → aspect 6.0; US spec aspect 1.0 → diff 5.0 far exceeds 15 % tolerance
        AnalysisResult result = service.analyzePhoto(toBytes(whiteImage(1200, 200)), "US");
        ComplianceCheck check = result.getChecks().get(1);
        assertThat(check.isPassed()).isFalse();
        assertThat(check.getMessage()).contains("centre-cropped");
    }

    @Test
    void checkAspectRatio_passes_atExactFifteenPercentBoundary() throws IOException {
        // US spec aspect = 1.0, tolerance = 0.15
        // 115×100 → imgAspect = 1.15, diff = 0.15 = tolerance → passes (condition is <=)
        AnalysisResult result = service.analyzePhoto(toBytes(whiteImage(115, 100)), "US");
        ComplianceCheck check = result.getChecks().get(1);
        assertThat(check.isPassed()).isTrue();
    }

    @Test
    void checkAspectRatio_fails_justAboveFifteenPercentBoundary() throws IOException {
        // 116×100 → imgAspect = 1.16, diff = 0.16 > 0.15 → fails
        AnalysisResult result = service.analyzePhoto(toBytes(whiteImage(116, 100)), "US");
        ComplianceCheck check = result.getChecks().get(1);
        assertThat(check.isPassed()).isFalse();
    }

    // ─── check[2]: Background Color ──────────────────────────────────────────────

    @Test
    void checkBackground_passes_withAllWhiteImage() throws IOException {
        AnalysisResult result = service.analyzePhoto(toBytes(whiteImage(600, 600)), "US");
        ComplianceCheck check = result.getChecks().get(2);
        assertThat(check.getRule()).isEqualTo("Background Color");
        assertThat(check.isPassed()).isTrue();
    }

    @Test
    void checkBackground_fails_withAllDarkImage() throws IOException {
        AnalysisResult result = service.analyzePhoto(toBytes(darkImage(600, 600)), "US");
        ComplianceCheck check = result.getChecks().get(2);
        assertThat(check.isPassed()).isFalse();
        assertThat(check.getMessage()).contains("too dark");
    }

    @Test
    void checkBackground_fails_withDarkCornersEvenWhenCenterIsWhite() throws IOException {
        // Real-world case: person photographed against a coloured wall — the centre has
        // skin tones but all four corner regions are dark/coloured → background check fails.
        BufferedImage img = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        // Leave corners black (default), fill only the inner rectangle white
        g.setColor(Color.WHITE);
        g.fillRect(50, 50, 500, 500);   // 50 px dark border survives at all four corners
        g.dispose();

        AnalysisResult result = service.analyzePhoto(toBytes(img), "US");
        ComplianceCheck check = result.getChecks().get(2);
        assertThat(check.getRule()).isEqualTo("Background Color");
        assertThat(check.isPassed()).isFalse();
    }

    // ─── check[3]: Resolution / Quality ─────────────────────────────────────────

    @Test
    void checkResolution_passes_whenPixelCountMeetsSpec() throws IOException {
        // 600×600 = 360 000 px  ≥  US spec 600×600 = 360 000 px
        AnalysisResult result = service.analyzePhoto(toBytes(whiteImage(600, 600)), "US");
        ComplianceCheck check = result.getChecks().get(3);
        assertThat(check.getRule()).isEqualTo("Resolution / Quality");
        assertThat(check.isPassed()).isTrue();
    }

    @Test
    void checkResolution_fails_whenPixelCountBelowSpec() throws IOException {
        // 100×100 = 10 000 px  <  US spec 360 000 px
        AnalysisResult result = service.analyzePhoto(toBytes(whiteImage(100, 100)), "US");
        ComplianceCheck check = result.getChecks().get(3);
        assertThat(check.isPassed()).isFalse();
    }

    // ─── check[4]: Face Presence ─────────────────────────────────────────────────

    @Test
    void checkFacePosition_passes_withHighVarianceCenterContent() throws IOException {
        AnalysisResult result = service.analyzePhoto(
            toBytes(variedCenterWhiteImage(600, 600)), "US");
        ComplianceCheck check = result.getChecks().get(4);
        assertThat(check.getRule()).isEqualTo("Face Presence (estimated)");
        assertThat(check.isPassed()).isTrue();
    }

    @Test
    void checkFacePosition_fails_withUniformWhiteImage() throws IOException {
        AnalysisResult result = service.analyzePhoto(toBytes(whiteImage(600, 600)), "US");
        ComplianceCheck check = result.getChecks().get(4);
        assertThat(check.isPassed()).isFalse();
        assertThat(check.getMessage()).contains("uniform");
    }

    // ─── allPassed flag ──────────────────────────────────────────────────────────

    @Test
    void allPassed_isTrueWhenAllFiveChecksPass() throws IOException {
        AnalysisResult result = service.analyzePhoto(
            toBytes(variedCenterWhiteImage(600, 600)), "US");
        assertThat(result.isAllPassed()).isTrue();
        assertThat(result.getPassedCount()).isEqualTo(5);
    }

    @Test
    void allPassed_isFalseWhenMultipleChecksFail() throws IOException {
        // 100×100 dark: fails dimensions, background, resolution, face-position
        AnalysisResult result = service.analyzePhoto(toBytes(darkImage(100, 100)), "US");
        assertThat(result.isAllPassed()).isFalse();
        assertThat(result.getPassedCount()).isLessThan(result.getTotalCount());
    }

    // ─── correctPhoto ────────────────────────────────────────────────────────────

    @Test
    void correctPhoto_returnsJpegWithExactTargetDimensions() throws IOException {
        byte[] corrected = service.correctPhoto(
            toBytes(variedCenterWhiteImage(800, 1000)), "US");
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(corrected));
        assertThat(out).isNotNull();
        assertThat(out.getWidth()).isEqualTo(600);
        assertThat(out.getHeight()).isEqualTo(600);
    }

    @Test
    void correctPhoto_withCustomSpec_returnsExactCustomDimensions() throws IOException {
        CountrySpec spec = new CountrySpec();
        spec.setWidthPx(413); spec.setHeightPx(531);
        spec.setDpi(300); spec.setBackgroundColorHex("#FFFFFF");

        byte[] corrected = service.correctPhoto(toBytes(variedCenterWhiteImage(600, 600)), spec);
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(corrected));
        assertThat(out.getWidth()).isEqualTo(413);
        assertThat(out.getHeight()).isEqualTo(531);
    }

    @Test
    void correctPhoto_withInvalidImage_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.correctPhoto("garbage".getBytes(), "US"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot read image");
    }

    // ─── generatePhotoSheet ──────────────────────────────────────────────────────

    @Test
    void generatePhotoSheet_returnsJpegSheetWithFixed1800x1200Dimensions() throws IOException {
        byte[] sheet = service.generatePhotoSheet(
            toBytes(variedCenterWhiteImage(600, 600)), "US");
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(sheet));
        assertThat(out).isNotNull();
        assertThat(out.getWidth()).isEqualTo(1800);
        assertThat(out.getHeight()).isEqualTo(1200);
    }

    @Test
    void generatePhotoSheet_withCustomSpec_returnsJpeg() throws IOException {
        CountrySpec spec = new CountrySpec();
        spec.setWidthPx(413); spec.setHeightPx(531);
        spec.setDpi(300); spec.setBackgroundColorHex("#FFFFFF");

        byte[] sheet = service.generatePhotoSheet(toBytes(variedCenterWhiteImage(500, 600)), spec);
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(sheet));
        assertThat(out).isNotNull();
        assertThat(out.getWidth()).isEqualTo(1800);
        assertThat(out.getHeight()).isEqualTo(1200);
    }

    @Test
    void generatePhotoSheet_withInvalidImage_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.generatePhotoSheet("garbage".getBytes(), "US"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot read image");
    }

    // ─── generatePhotoPdf ────────────────────────────────────────────────────────

    @Test
    void generatePhotoPdf_returnsNonEmptyPdfBytes() throws IOException {
        byte[] pdf = service.generatePhotoPdf(
            toBytes(variedCenterWhiteImage(600, 600)), "US");
        assertThat(pdf).isNotEmpty();
        // PDF magic bytes: %PDF
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generatePhotoPdf_withCustomSpec_returnsPdf() throws IOException {
        CountrySpec spec = new CountrySpec();
        spec.setWidthPx(413); spec.setHeightPx(531);
        spec.setWidthMm(35); spec.setHeightMm(45);
        spec.setDpi(300); spec.setBackgroundColorHex("#FFFFFF");

        byte[] pdf = service.generatePhotoPdf(toBytes(variedCenterWhiteImage(500, 600)), spec);
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generatePhotoPdf_withInvalidImage_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.generatePhotoPdf("garbage".getBytes(), "US"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot read image");
    }
}
