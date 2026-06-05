package com.passport.photo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.passport.photo.model.AnalysisResult;
import com.passport.photo.model.ComplianceCheck;
import com.passport.photo.model.CountrySpec;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Core service for passport photo compliance analysis and image correction.
 *
 * <p>On construction, loads all country specifications from
 * {@code /country-specs.json} on the classpath. Exposes methods to run five
 * compliance checks against an uploaded image and to produce a corrected JPEG
 * that exactly matches the target country's pixel dimensions.</p>
 *
 * <h2>Compliance checks (in order)</h2>
 * <ol>
 *   <li>Image Dimensions — width and height meet or exceed the required pixel size</li>
 *   <li>Aspect Ratio — within ±15% of the target ratio</li>
 *   <li>Background Color — corner sampling: &gt;75% of sampled corner pixels are bright (R,G,B &gt; 200)</li>
 *   <li>Resolution / Quality — total pixel area meets or exceeds the required area</li>
 *   <li>Face Presence (estimated) — colour variance in the upper-centre region exceeds a threshold</li>
 * </ol>
 *
 * @version 1.0
 */
@Service
public class PhotoAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PhotoAnalysisService.class);

    private final Map<String, CountrySpec> countrySpecs;

    /**
     * Loads country specifications from {@code country-specs.json} on the classpath.
     *
     * @throws RuntimeException if the file is missing or cannot be parsed
     */
    public PhotoAnalysisService() {
        try (InputStream is = getClass().getResourceAsStream("/country-specs.json")) {
            if (is == null) throw new RuntimeException("country-specs.json not found on classpath");
            countrySpecs = new ObjectMapper().readValue(is, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to load country specs", e);
        }
    }

    /**
     * Analyzes a photo against the built-in spec for the given country code.
     *
     * @param imageBytes raw bytes of the uploaded image (JPEG, PNG, or WEBP)
     * @param country    country code key (e.g. {@code "US"}, {@code "India"})
     * @return the compliance analysis result
     * @throws IllegalArgumentException if the country code is unknown or the image cannot be read
     * @throws IOException              if an I/O error occurs reading the image bytes
     */
    public AnalysisResult analyzePhoto(byte[] imageBytes, String country) throws IOException {
        return analyzePhoto(imageBytes, getSpec(country), country);
    }

    /**
     * Analyzes a photo against an explicitly supplied spec (used for Custom country).
     *
     * @param imageBytes raw bytes of the uploaded image
     * @param spec       the {@link CountrySpec} to evaluate against
     * @return the compliance analysis result
     * @throws IllegalArgumentException if the image cannot be decoded
     * @throws IOException              if an I/O error occurs reading the image bytes
     */
    public AnalysisResult analyzePhoto(byte[] imageBytes, CountrySpec spec) throws IOException {
        String label = (spec.getName() != null && !spec.getName().isBlank()) ? spec.getName() : "Custom";
        return analyzePhoto(imageBytes, spec, label);
    }

    /**
     * Internal analysis driver — runs all five checks and assembles the result.
     *
     * @param imageBytes raw bytes of the uploaded image
     * @param spec       the spec to evaluate against
     * @param label      display label for the country in the result
     * @return the compliance analysis result
     * @throws IllegalArgumentException if the image cannot be decoded
     * @throws IOException              if an I/O error occurs
     */
    private AnalysisResult analyzePhoto(byte[] imageBytes, CountrySpec spec, String label) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) throw new IllegalArgumentException("Cannot read image — please upload a valid JPEG or PNG.");

        List<ComplianceCheck> checks = new ArrayList<>();
        checks.add(checkDimensions(image, spec));
        checks.add(checkAspectRatio(image, spec));
        checks.add(checkBackground(image, spec));
        checks.add(checkResolution(image, spec));
        checks.add(checkFacePosition(image));

        long passed = checks.stream().filter(ComplianceCheck::isPassed).count();
        return new AnalysisResult(label, spec, checks, passed == checks.size(), (int) passed, checks.size());
    }

    /**
     * Corrects a photo to exactly match the pixel dimensions of the built-in spec
     * for the given country code.
     *
     * @param imageBytes raw bytes of the uploaded image
     * @param country    country code key (e.g. {@code "UK"})
     * @return JPEG bytes of the corrected image
     * @throws IllegalArgumentException if the country code is unknown or the image cannot be read
     * @throws IOException              if an I/O error occurs
     */
    public byte[] correctPhoto(byte[] imageBytes, String country) throws IOException {
        return correctPhoto(imageBytes, getSpec(country));
    }

    /**
     * Corrects a photo to exactly match the pixel dimensions specified in the supplied spec.
     *
     * <p>Uses cover-mode scaling (both dimensions filled) followed by a centre crop with a
     * slight top-bias vertical offset to keep the face in frame, then fills remaining canvas
     * area with the spec's background colour.</p>
     *
     * @param imageBytes raw bytes of the uploaded image
     * @param spec       the target {@link CountrySpec}
     * @return JPEG bytes of the corrected image
     * @throws IllegalArgumentException if the image cannot be decoded
     * @throws IOException              if an I/O error occurs
     */
    public byte[] correctPhoto(byte[] imageBytes, CountrySpec spec) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) throw new IllegalArgumentException("Cannot read image — please upload a valid JPEG or PNG.");

        BufferedImage corrected = buildCorrectedImage(original, spec);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(corrected, "jpg", baos);
        return baos.toByteArray();
    }

    /**
     * Generates a print-ready 4×6" sheet (1800×1200 px at 300 DPI) tiling as many
     * copies of the corrected passport photo as fit with a 15 px gutter.
     *
     * @param imageBytes raw bytes of the uploaded image
     * @param country    country code key (e.g. {@code "US"})
     * @return JPEG bytes of the print sheet
     * @throws IllegalArgumentException if the country code is unknown or the image cannot be read
     * @throws IOException              if an I/O error occurs
     */
    public byte[] generatePhotoSheet(byte[] imageBytes, String country) throws IOException {
        return generatePhotoSheet(imageBytes, getSpec(country));
    }

    /**
     * Generates a print-ready 4×6" sheet tiling corrected passport photos.
     *
     * @param imageBytes raw bytes of the uploaded image
     * @param spec       the target {@link CountrySpec}
     * @return JPEG bytes of the print sheet
     * @throws IllegalArgumentException if the image cannot be decoded
     * @throws IOException              if an I/O error occurs
     */
    public byte[] generatePhotoSheet(byte[] imageBytes, CountrySpec spec) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) throw new IllegalArgumentException("Cannot read image — please upload a valid JPEG or PNG.");

        BufferedImage corrected = buildCorrectedImage(original, spec);

        int pw = spec.getWidthPx(), ph = spec.getHeightPx();
        int sheetW = 1800, sheetH = 1200;  // 4×6" landscape at 300 DPI
        int gutter = 15;

        int cols = Math.max(1, (sheetW + gutter) / (pw + gutter));
        int rows = Math.max(1, (sheetH + gutter) / (ph + gutter));

        int usedW = cols * pw + (cols - 1) * gutter;
        int usedH = rows * ph + (rows - 1) * gutter;
        int offsetX = (sheetW - usedW) / 2;
        int offsetY = (sheetH - usedH) / 2;

        BufferedImage sheet = new BufferedImage(sheetW, sheetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, sheetW, sheetH);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = offsetX + col * (pw + gutter);
                int y = offsetY + row * (ph + gutter);
                g.drawImage(corrected, x, y, pw, ph, null);
            }
        }
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(sheet, "jpg", baos);
        return baos.toByteArray();
    }

    /**
     * Generates a print-ready A4 PDF tiling as many copies of the corrected passport photo
     * as fit with an 8 pt gutter (~3 mm). Photos are sized to their physical mm dimensions
     * at 72 pt/inch so the PDF prints at the correct size on any A4 printer.
     *
     * @param imageBytes raw bytes of the uploaded image
     * @param country    country code key (e.g. {@code "UK"})
     * @return PDF bytes ready for download
     * @throws IllegalArgumentException if the country code is unknown or the image cannot be read
     * @throws IOException              if an I/O error occurs
     */
    public byte[] generatePhotoPdf(byte[] imageBytes, String country) throws IOException {
        return generatePhotoPdf(imageBytes, getSpec(country));
    }

    /**
     * Generates a print-ready A4 PDF tiling corrected passport photos.
     *
     * @param imageBytes raw bytes of the uploaded image
     * @param spec       the target {@link CountrySpec}
     * @return PDF bytes ready for download
     * @throws IllegalArgumentException if the image cannot be decoded
     * @throws IOException              if an I/O error occurs
     */
    public byte[] generatePhotoPdf(byte[] imageBytes, CountrySpec spec) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) throw new IllegalArgumentException("Cannot read image — please upload a valid JPEG or PNG.");

        BufferedImage corrected = buildCorrectedImage(original, spec);

        // Convert mm → PDF points (72 pt = 1 inch = 25.4 mm)
        float photoWPt = (float) (spec.getWidthMm()  / 25.4 * 72);
        float photoHPt = (float) (spec.getHeightMm() / 25.4 * 72);
        float gutterPt = 8f;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float pageW = page.getMediaBox().getWidth();   // 595.28 pt
            float pageH = page.getMediaBox().getHeight();  // 841.89 pt

            int cols = Math.max(1, (int) ((pageW + gutterPt) / (photoWPt + gutterPt)));
            int rows = Math.max(1, (int) ((pageH + gutterPt) / (photoHPt + gutterPt)));

            float usedW = cols * photoWPt + (cols - 1) * gutterPt;
            float usedH = rows * photoHPt + (rows - 1) * gutterPt;
            float offsetX = (pageW - usedW) / 2f;
            float offsetY = (pageH - usedH) / 2f;

            PDImageXObject pdImage = JPEGFactory.createFromImage(document, corrected);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        float x = offsetX + col * (photoWPt + gutterPt);
                        // PDFBox origin is bottom-left; row 0 is the top row on the page
                        float y = pageH - offsetY - (row + 1) * photoHPt - row * gutterPt;
                        cs.drawImage(pdImage, x, y, photoWPt, photoHPt);
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Returns the full map of built-in country specifications keyed by country code.
     *
     * @return unmodifiable view of the loaded country specs
     */
    public Map<String, CountrySpec> getCountrySpecs() {
        return Collections.unmodifiableMap(countrySpecs);
    }

    /**
     * Looks up a built-in country spec by code.
     *
     * @param country country code key
     * @return the matching {@link CountrySpec}
     * @throws IllegalArgumentException if no spec exists for the given code
     */
    private CountrySpec getSpec(String country) {
        CountrySpec spec = countrySpecs.get(country);
        if (spec == null) throw new IllegalArgumentException("Unknown country code: " + country);
        return spec;
    }

    /**
     * Checks that the image is at least as wide and tall as the spec requires.
     *
     * @param image the decoded source image
     * @param spec  the country spec defining minimum dimensions
     * @return a {@link ComplianceCheck} with {@code passed=true} if both dimensions meet the minimum
     */
    private ComplianceCheck checkDimensions(BufferedImage image, CountrySpec spec) {
        boolean passed = image.getWidth() >= spec.getWidthPx() && image.getHeight() >= spec.getHeightPx();
        String actual   = image.getWidth() + "×" + image.getHeight() + " px";
        String expected = "≥ " + spec.getWidthPx() + "×" + spec.getHeightPx() + " px";
        String message;
        if (passed) {
            message = "Image meets the minimum pixel dimensions";
        } else {
            double scale = Math.max(
                    (double) spec.getWidthPx()  / image.getWidth(),
                    (double) spec.getHeightPx() / image.getHeight());
            message = String.format(
                    "Image is %.1f× too small — correction will upscale and may reduce quality", scale);
        }
        return new ComplianceCheck("Image Dimensions", passed, message, expected, actual);
    }

    /**
     * Checks that the image aspect ratio is within ±15% of the target ratio.
     *
     * @param image the decoded source image
     * @param spec  the country spec defining the target dimensions
     * @return a {@link ComplianceCheck} indicating whether the ratio is acceptable
     */
    private ComplianceCheck checkAspectRatio(BufferedImage image, CountrySpec spec) {
        double imgAspect  = (double) image.getWidth()  / image.getHeight();
        double specAspect = (double) spec.getWidthPx() / spec.getHeightPx();
        boolean passed = Math.abs(imgAspect - specAspect) <= specAspect * 0.15;
        String actual   = String.format("%.2f : 1", imgAspect);
        String expected = String.format("%.2f : 1  (±15%%)", specAspect);
        return new ComplianceCheck("Aspect Ratio", passed,
                passed ? "Aspect ratio is within the acceptable range"
                       : "Aspect ratio differs — photo will be centre-cropped to fit",
                expected, actual);
    }

    /**
     * Checks that the background appears white or light-coloured by sampling corner pixels.
     *
     * <p>Samples a square region of up to 30 × 30 pixels in each of the four corners and
     * counts how many pixels have R, G, and B all above 200. Passes if more than 75% of
     * sampled pixels meet that threshold.</p>
     *
     * @param image the decoded source image
     * @param spec  the country spec (used for the expected background colour in the message)
     * @return a {@link ComplianceCheck} indicating whether the background is sufficiently light
     */
    private ComplianceCheck checkBackground(BufferedImage image, CountrySpec spec) {
        int radius = Math.min(30, Math.min(image.getWidth(), image.getHeight()) / 8);
        if (radius == 0) {
            return new ComplianceCheck("Background Color", false,
                    "Image is too small to sample background — ensure a plain white/light background",
                    spec.getBackgroundColorHex() + " (plain light)", "Image too small to sample");
        }
        int bright = 0, total = 0;
        for (int x = 0; x < radius; x++) {
            for (int y = 0; y < radius; y++) {
                int[] cornerPixels = {
                    image.getRGB(x, y),
                    image.getRGB(image.getWidth()  - 1 - x, y),
                    image.getRGB(x,                          image.getHeight() - 1 - y),
                    image.getRGB(image.getWidth()  - 1 - x, image.getHeight() - 1 - y)
                };
                for (int rgb : cornerPixels) {
                    Color c = new Color(rgb, true);
                    if (c.getRed() > 200 && c.getGreen() > 200 && c.getBlue() > 200) bright++;
                    total++;
                }
            }
        }
        double ratio  = (double) bright / total;
        boolean passed = ratio > 0.75;
        return new ComplianceCheck("Background Color", passed,
                passed ? "Background appears white or light-coloured"
                       : "Background may be too dark or coloured — use a plain white/light background",
                spec.getBackgroundColorHex() + " (plain light)",
                String.format("%.0f%% light pixels sampled at corners", ratio * 100));
    }

    /**
     * Checks that the total pixel count meets or exceeds the required area.
     *
     * <p>Note: this compares pixel counts only — it does not read or validate EXIF DPI metadata.</p>
     *
     * @param image the decoded source image
     * @param spec  the country spec defining the required pixel dimensions
     * @return a {@link ComplianceCheck} indicating whether the resolution is sufficient
     */
    private ComplianceCheck checkResolution(BufferedImage image, CountrySpec spec) {
        long minPx = (long) spec.getWidthPx()  * spec.getHeightPx();
        long actPx = (long) image.getWidth()   * image.getHeight();
        boolean passed = actPx >= minPx;
        return new ComplianceCheck("Resolution / Quality", passed,
                passed ? "Resolution is sufficient for a quality print at " + spec.getDpi() + " DPI"
                       : "Resolution is lower than recommended — printed photo may appear blurry",
                spec.getDpi() + " DPI  (" + spec.getWidthPx() + "×" + spec.getHeightPx() + " px)",
                image.getWidth() + "×" + image.getHeight() + " px");
    }

    /**
     * Estimates whether a face is present and centred by measuring pixel colour variance.
     *
     * <p>Samples the red channel in the upper-centre quarter of the image (x: 25–75%,
     * y: 12.5–62.5%) and computes variance. Variance above 200 is treated as evidence
     * of image content (i.e. a face). Near-zero variance suggests a blank or uniformly
     * coloured region.</p>
     *
     * <p><strong>Limitation:</strong> this is a heuristic, not a true face detector. It can
     * produce false negatives on very uniform photos and false positives on patterned
     * backgrounds.</p>
     *
     * @param image the decoded source image
     * @return a {@link ComplianceCheck} estimating whether a face is centred in the upper region
     */
    private ComplianceCheck checkFacePosition(BufferedImage image) {
        // Heuristic: sample colour variance in the upper-centre region (where a face would be).
        // High variance signals image content; near-zero variance suggests a blank / uniform area.
        int x0 = image.getWidth()  / 4,  x1 = 3 * image.getWidth()  / 4;
        int y0 = image.getHeight() / 8,  y1 = 5 * image.getHeight() / 8;
        int step = Math.max(1, (x1 - x0) / 20);
        List<Integer> reds = new ArrayList<>();
        for (int x = x0; x < x1; x += step)
            for (int y = y0; y < y1; y += step)
                reds.add(new Color(image.getRGB(x, y)).getRed());

        double avg = reds.stream().mapToInt(Integer::intValue).average().orElse(128);
        double var = reds.stream().mapToDouble(r -> (r - avg) * (r - avg)).average().orElse(0);
        boolean hasContent = var > 200;
        return new ComplianceCheck("Face Presence (estimated)", hasContent,
                hasContent ? "Image centre has visible content — face appears to be present"
                           : "Centre of image appears uniform — ensure your face is clearly centred",
                "Centred face in upper-centre region",
                hasContent ? "Content detected" : "Low variation detected in centre");
    }

    /**
     * Scales and crops the source image to exactly match the spec's target dimensions.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Compute a scale factor so that both target dimensions are fully covered
     *       (cover mode — no empty borders).</li>
     *   <li>Scale the source using Thumbnailator with bicubic interpolation.</li>
     *   <li>Centre-crop horizontally; apply a 1/3 top-bias vertical crop so the face
     *       stays in frame rather than being centred on empty lower area.</li>
     *   <li>Fill the canvas with the spec's background colour before drawing the image
     *       so any residual border is the correct colour.</li>
     * </ol>
     * </p>
     *
     * @param src  the original decoded image
     * @param spec the target country spec
     * @return a new {@link BufferedImage} with exactly {@code widthPx × heightPx} pixels
     * @throws IOException if Thumbnailator fails to scale the image
     */
    private BufferedImage buildCorrectedImage(BufferedImage src, CountrySpec spec) throws IOException {
        int tw = spec.getWidthPx(), th = spec.getHeightPx();

        // Scale to "cover" mode — both target dimensions are fully filled
        double scale = Math.max((double) tw / src.getWidth(), (double) th / src.getHeight());
        int sw = (int) Math.ceil(src.getWidth()  * scale);
        int sh = (int) Math.ceil(src.getHeight() * scale);

        BufferedImage scaled = Thumbnails.of(src)
                .size(sw, sh)
                .keepAspectRatio(false)
                .asBufferedImage();

        BufferedImage canvas = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill background
        try { g.setColor(Color.decode(spec.getBackgroundColorHex())); }
        catch (NumberFormatException e) {
            log.warn("Invalid backgroundColorHex '{}' for spec '{}' — falling back to white",
                    spec.getBackgroundColorHex(), spec.getName());
            g.setColor(Color.WHITE);
        }
        g.fillRect(0, 0, tw, th);

        // Centre-horizontally; slight top-bias vertically to keep face in frame
        int dx = -(sw - tw) / 2;
        int dy = -(sh - th) / 3;
        g.drawImage(scaled, dx, dy, null);
        g.dispose();

        return canvas;
    }
}
