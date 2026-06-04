package com.passport.photo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passport.photo.model.AnalysisResult;
import com.passport.photo.model.CountrySpec;
import com.passport.photo.service.FaceDetectionService;
import com.passport.photo.service.PhotoAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * REST controller exposing the Passport Photo Corrector API.
 *
 * <p>Handles three endpoints:
 * <ul>
 *   <li>{@code GET  /api/countries} — list all built-in country specs</li>
 *   <li>{@code POST /api/analyze}   — run compliance checks on an uploaded photo</li>
 *   <li>{@code POST /api/correct}   — return a corrected photo as JPEG bytes</li>
 * </ul>
 * Both POST endpoints validate the file MIME type and reject photos that
 * contain more than one face before delegating to the service layer.</p>
 *
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
public class PhotoController {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final int MAX_SPEC_JSON_LEN   = 4_096;
    private static final int MAX_SPEC_DIMENSION  = 3_000;

    @Autowired
    private PhotoAnalysisService analysisService;

    @Autowired
    private FaceDetectionService faceDetectionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Returns all built-in country specifications.
     *
     * @return 200 OK with a map of country code → {@link CountrySpec}
     */
    @GetMapping("/countries")
    public ResponseEntity<Map<String, CountrySpec>> getCountries() {
        return ResponseEntity.ok(analysisService.getCountrySpecs());
    }

    /**
     * Analyzes a photo against a country's passport photo requirements.
     *
     * <p>When {@code country=Custom} and {@code customSpec} is provided, the spec
     * is parsed from the JSON string and used directly. Otherwise the spec is
     * looked up from the built-in country map.</p>
     *
     * @param photo         the image file (JPEG, PNG, or WEBP)
     * @param country       country code, e.g. {@code "US"}, or {@code "Custom"}
     * @param customSpecJson optional JSON string of a {@link CountrySpec}; required when
     *                       {@code country=Custom}
     * @return 200 with an {@link AnalysisResult}; 400 on validation errors; 500 on I/O failures
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzePhoto(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("country") String country,
            @RequestParam(value = "customSpec", required = false) String customSpecJson) {
        byte[] bytes = null;
        try {
            ResponseEntity<?> inputError = validateCustomSpecInput(country, customSpecJson);
            if (inputError != null) return inputError;

            ResponseEntity<?> typeError = validateImageType(photo);
            if (typeError != null) return typeError;

            bytes = photo.getBytes();

            ResponseEntity<?> faceError = checkFaces(bytes);
            if (faceError != null) return faceError;

            AnalysisResult result;
            if (customSpecJson != null && !customSpecJson.isBlank()) {
                CountrySpec spec = objectMapper.readValue(customSpecJson, CountrySpec.class);
                ResponseEntity<?> specError = validateCustomSpec(spec);
                if (specError != null) return specError;
                result = analysisService.analyzePhoto(bytes, spec);
            } else {
                result = analysisService.analyzePhoto(bytes, country);
            }
            return ResponseEntity.ok()
                    .header("X-Privacy-Notice", "Photo data is processed in-memory only and is not stored on disk.")
                    .body(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to process image: " + e.getMessage()));
        } finally {
            wipeBytes(bytes);
        }
    }

    /**
     * Corrects a photo to exactly match a country's required pixel dimensions.
     *
     * <p>Applies cover-mode scaling followed by a centre crop and background fill.
     * Returns the corrected image as a JPEG file download.</p>
     *
     * @param photo         the image file (JPEG, PNG, or WEBP)
     * @param country       country code, e.g. {@code "UK"}, or {@code "Custom"}
     * @param customSpecJson optional JSON string of a {@link CountrySpec}; required when
     *                       {@code country=Custom}
     * @return 200 with JPEG bytes and {@code Content-Disposition: attachment}; 400/500 on errors
     */
    @PostMapping(value = "/correct", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> correctPhoto(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("country") String country,
            @RequestParam(value = "customSpec", required = false) String customSpecJson) {
        byte[] bytes = null;
        try {
            ResponseEntity<?> inputError = validateCustomSpecInput(country, customSpecJson);
            if (inputError != null) return inputError;

            ResponseEntity<?> typeError = validateImageType(photo);
            if (typeError != null) return typeError;

            bytes = photo.getBytes();

            ResponseEntity<?> faceError = checkFaces(bytes);
            if (faceError != null) return faceError;

            byte[] corrected;
            if (customSpecJson != null && !customSpecJson.isBlank()) {
                CountrySpec spec = objectMapper.readValue(customSpecJson, CountrySpec.class);
                ResponseEntity<?> specError = validateCustomSpec(spec);
                if (specError != null) return specError;
                corrected = analysisService.correctPhoto(bytes, spec);
            } else {
                corrected = analysisService.correctPhoto(bytes, country);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header("Content-Disposition", "attachment; filename=passport_photo.jpg")
                    .header("X-Privacy-Notice", "Photo data is processed in-memory only and is not stored on disk.")
                    .body(corrected);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to correct image: " + e.getMessage()));
        } finally {
            wipeBytes(bytes);
        }
    }

    /**
     * Generates a print-ready A4 PDF with multiple copies of the corrected photo tiled at
     * their physical mm dimensions (72 pt/inch), ready to print on any A4 printer.
     *
     * @param photo         the image file (JPEG, PNG, or WEBP)
     * @param country       country code, e.g. {@code "US"}, or {@code "Custom"}
     * @param customSpecJson optional JSON string of a {@link CountrySpec}; required when
     *                       {@code country=Custom}
     * @return 200 with PDF bytes and {@code Content-Disposition: attachment}; 400/500 on errors
     */
    @PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> photoPdf(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("country") String country,
            @RequestParam(value = "customSpec", required = false) String customSpecJson) {
        byte[] bytes = null;
        try {
            ResponseEntity<?> inputError = validateCustomSpecInput(country, customSpecJson);
            if (inputError != null) return inputError;

            ResponseEntity<?> typeError = validateImageType(photo);
            if (typeError != null) return typeError;

            bytes = photo.getBytes();

            ResponseEntity<?> faceError = checkFaces(bytes);
            if (faceError != null) return faceError;

            byte[] pdf;
            if (customSpecJson != null && !customSpecJson.isBlank()) {
                CountrySpec spec = objectMapper.readValue(customSpecJson, CountrySpec.class);
                ResponseEntity<?> specError = validateCustomSpec(spec);
                if (specError != null) return specError;
                pdf = analysisService.generatePhotoPdf(bytes, spec);
            } else {
                pdf = analysisService.generatePhotoPdf(bytes, country);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=passport_photo_sheet.pdf")
                    .header("X-Privacy-Notice", "Photo data is processed in-memory only and is not stored on disk.")
                    .body(pdf);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to generate PDF: " + e.getMessage()));
        } finally {
            wipeBytes(bytes);
        }
    }

    /**
     * Generates a print-ready 4×6" photo sheet with multiple copies of the corrected photo.
     *
     * <p>Tiles as many copies as fit on a 1800×1200 px (landscape 4×6" at 300 DPI) canvas
     * with a 15 px gutter between photos. Returns the sheet as a downloadable JPEG.</p>
     *
     * @param photo         the image file (JPEG, PNG, or WEBP)
     * @param country       country code, e.g. {@code "US"}, or {@code "Custom"}
     * @param customSpecJson optional JSON string of a {@link CountrySpec}; required when
     *                       {@code country=Custom}
     * @return 200 with JPEG bytes and {@code Content-Disposition: attachment}; 400/500 on errors
     */
    @PostMapping(value = "/sheet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> photoSheet(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("country") String country,
            @RequestParam(value = "customSpec", required = false) String customSpecJson) {
        byte[] bytes = null;
        try {
            ResponseEntity<?> inputError = validateCustomSpecInput(country, customSpecJson);
            if (inputError != null) return inputError;

            ResponseEntity<?> typeError = validateImageType(photo);
            if (typeError != null) return typeError;

            bytes = photo.getBytes();

            ResponseEntity<?> faceError = checkFaces(bytes);
            if (faceError != null) return faceError;

            byte[] sheet;
            if (customSpecJson != null && !customSpecJson.isBlank()) {
                CountrySpec spec = objectMapper.readValue(customSpecJson, CountrySpec.class);
                ResponseEntity<?> specError = validateCustomSpec(spec);
                if (specError != null) return specError;
                sheet = analysisService.generatePhotoSheet(bytes, spec);
            } else {
                sheet = analysisService.generatePhotoSheet(bytes, country);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header("Content-Disposition", "attachment; filename=passport_photo_sheet.jpg")
                    .header("X-Privacy-Notice", "Photo data is processed in-memory only and is not stored on disk.")
                    .body(sheet);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to generate photo sheet: " + e.getMessage()));
        } finally {
            wipeBytes(bytes);
        }
    }

    /**
     * Returns a 400 error response if the file's MIME type is not JPEG, PNG, or WEBP.
     *
     * @param file the uploaded file
     * @return an error {@link ResponseEntity}, or {@code null} if the type is acceptable
     */
    private ResponseEntity<?> validateImageType(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_TYPES.contains(ct)) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Unsupported file type. Please upload a JPEG, PNG, or WEBP image."
            ));
        }
        return null;
    }

    /**
     * Returns a 400 error response if the image contains more than one face.
     *
     * <p>Only the multiple-people case is blocked here. Photos with zero detected
     * faces are allowed through; face presence is reported as a compliance check
     * by the analysis service instead.</p>
     *
     * @param bytes raw image bytes
     * @return an error {@link ResponseEntity} with {@code errorType=MULTIPLE_FACES},
     *         or {@code null} if zero or one face was detected
     */
    private ResponseEntity<?> checkFaces(byte[] bytes) {
        int faces = faceDetectionService.countFaces(bytes);
        if (faces > 1) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", faces + " people detected. Passport photos must show exactly one person — please upload a solo photo.",
                "errorType", "MULTIPLE_FACES"
            ));
        }
        return null;
    }

    private void wipeBytes(byte[] bytes) {
        if (bytes != null) {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    /**
     * Returns a 400 if the custom spec JSON is too large, the country is "Custom" with no spec,
     * or the parsed spec has invalid/dangerous dimension or colour values.
     *
     * @param country        the country code from the request
     * @param customSpecJson the raw JSON string (may be null)
     * @return an error {@link ResponseEntity}, or {@code null} if everything is valid
     */
    private ResponseEntity<?> validateCustomSpecInput(String country, String customSpecJson) {
        if ("Custom".equals(country) && (customSpecJson == null || customSpecJson.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("message",
                "A customSpec JSON body is required when country=Custom."));
        }
        if (customSpecJson != null && customSpecJson.length() > MAX_SPEC_JSON_LEN) {
            return ResponseEntity.badRequest().body(Map.of("message",
                "customSpec JSON must not exceed " + MAX_SPEC_JSON_LEN + " characters."));
        }
        return null;
    }

    /**
     * Validates the parsed {@link CountrySpec} for safe dimension and colour values.
     *
     * @param spec the parsed spec to validate
     * @return an error {@link ResponseEntity}, or {@code null} if the spec is acceptable
     */
    private ResponseEntity<?> validateCustomSpec(CountrySpec spec) {
        if (spec.getWidthPx() <= 0 || spec.getHeightPx() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message",
                "Custom spec: widthPx and heightPx must be greater than 0."));
        }
        if (spec.getWidthPx() > MAX_SPEC_DIMENSION || spec.getHeightPx() > MAX_SPEC_DIMENSION) {
            return ResponseEntity.badRequest().body(Map.of("message",
                "Custom spec: dimensions cannot exceed " + MAX_SPEC_DIMENSION + " × " + MAX_SPEC_DIMENSION + " px."));
        }
        String hex = spec.getBackgroundColorHex();
        if (hex == null || !hex.matches("#[0-9A-Fa-f]{6}")) {
            return ResponseEntity.badRequest().body(Map.of("message",
                "Custom spec: backgroundColorHex must be a valid 6-digit hex colour (e.g. #FFFFFF)."));
        }
        return null;
    }
}
