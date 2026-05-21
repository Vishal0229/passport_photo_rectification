package com.passport.photo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.passport.photo.model.AnalysisResult;
import com.passport.photo.model.ComplianceCheck;
import com.passport.photo.model.CountrySpec;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PhotoAnalysisService {

    private final Map<String, CountrySpec> countrySpecs;

    public PhotoAnalysisService() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream("/country-specs.json");
            if (is == null) throw new RuntimeException("country-specs.json not found on classpath");
            countrySpecs = mapper.readValue(is, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to load country specs", e);
        }
    }

    public AnalysisResult analyzePhoto(MultipartFile photo, String country) throws IOException {
        CountrySpec spec = getSpec(country);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(photo.getBytes()));
        if (image == null) throw new IllegalArgumentException("Cannot read image — please upload a valid JPEG or PNG.");

        List<ComplianceCheck> checks = new ArrayList<>();
        checks.add(checkDimensions(image, spec));
        checks.add(checkAspectRatio(image, spec));
        checks.add(checkBackground(image, spec));
        checks.add(checkResolution(image, spec));
        checks.add(checkFacePosition(image));

        long passed = checks.stream().filter(ComplianceCheck::isPassed).count();
        return new AnalysisResult(country, spec, checks, passed == checks.size(), (int) passed, checks.size());
    }

    public byte[] correctPhoto(MultipartFile photo, String country) throws IOException {
        CountrySpec spec = getSpec(country);
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(photo.getBytes()));
        if (original == null) throw new IllegalArgumentException("Cannot read image — please upload a valid JPEG or PNG.");

        BufferedImage corrected = buildCorrectedImage(original, spec);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(corrected, "jpg", baos);
        return baos.toByteArray();
    }

    public Map<String, CountrySpec> getCountrySpecs() {
        return countrySpecs;
    }

    private CountrySpec getSpec(String country) {
        CountrySpec spec = countrySpecs.get(country);
        if (spec == null) throw new IllegalArgumentException("Unknown country code: " + country);
        return spec;
    }

    private ComplianceCheck checkDimensions(BufferedImage image, CountrySpec spec) {
        boolean passed = image.getWidth() >= spec.getWidthPx() && image.getHeight() >= spec.getHeightPx();
        String actual   = image.getWidth() + "×" + image.getHeight() + " px";
        String expected = "≥ " + spec.getWidthPx() + "×" + spec.getHeightPx() + " px";
        return new ComplianceCheck("Image Dimensions", passed,
                passed ? "Image meets the minimum pixel dimensions"
                       : "Image is smaller than required — the corrected photo may be upscaled",
                expected, actual);
    }

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

    private ComplianceCheck checkBackground(BufferedImage image, CountrySpec spec) {
        int radius = Math.min(30, Math.min(image.getWidth(), image.getHeight()) / 8);
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
        catch (NumberFormatException e) { g.setColor(Color.WHITE); }
        g.fillRect(0, 0, tw, th);

        // Centre-horizontally; slight top-bias vertically to keep face in frame
        int dx = -(sw - tw) / 2;
        int dy = -(sh - th) / 3;
        g.drawImage(scaled, dx, dy, null);
        g.dispose();

        return canvas;
    }
}
