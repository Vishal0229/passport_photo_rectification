package com.passport.photo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passport.photo.model.AnalysisResult;
import com.passport.photo.model.CountrySpec;
import com.passport.photo.service.FaceDetectionService;
import com.passport.photo.service.PhotoAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PhotoController {

    @Autowired
    private PhotoAnalysisService analysisService;

    @Autowired
    private FaceDetectionService faceDetectionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/countries")
    public ResponseEntity<Map<String, CountrySpec>> getCountries() {
        return ResponseEntity.ok(analysisService.getCountrySpecs());
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzePhoto(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("country") String country,
            @RequestParam(value = "customSpec", required = false) String customSpecJson) {
        try {
            byte[] bytes = photo.getBytes();

            int faces = faceDetectionService.countFaces(bytes);
            if (faces > 1) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", faces + " people detected. Passport photos must show exactly one person — please upload a solo photo.",
                    "errorType", "MULTIPLE_FACES"
                ));
            }

            AnalysisResult result;
            if (customSpecJson != null && !customSpecJson.isBlank()) {
                CountrySpec spec = objectMapper.readValue(customSpecJson, CountrySpec.class);
                result = analysisService.analyzePhoto(photo, spec);
            } else {
                result = analysisService.analyzePhoto(photo, country);
            }
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to process image: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/correct", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> correctPhoto(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("country") String country,
            @RequestParam(value = "customSpec", required = false) String customSpecJson) {
        try {
            byte[] bytes = photo.getBytes();

            int faces = faceDetectionService.countFaces(bytes);
            if (faces > 1) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", faces + " people detected. Passport photos must show exactly one person — please upload a solo photo.",
                    "errorType", "MULTIPLE_FACES"
                ));
            }

            byte[] corrected;
            if (customSpecJson != null && !customSpecJson.isBlank()) {
                CountrySpec spec = objectMapper.readValue(customSpecJson, CountrySpec.class);
                corrected = analysisService.correctPhoto(photo, spec);
            } else {
                corrected = analysisService.correctPhoto(photo, country);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentDispositionFormData("attachment", "passport_photo.jpg");
            return ResponseEntity.ok().headers(headers).body(corrected);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to correct image: " + e.getMessage()));
        }
    }
}
