package com.passport.photo.controller;

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

    @GetMapping("/countries")
    public ResponseEntity<Map<String, CountrySpec>> getCountries() {
        return ResponseEntity.ok(analysisService.getCountrySpecs());
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzePhoto(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("country") String country) {
        try {
            byte[] bytes = photo.getBytes();

            int faces = faceDetectionService.countFaces(bytes);
            if (faces == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "No face detected. Please upload a clear photo of a person facing the camera.",
                    "errorType", "NO_FACE"
                ));
            }

            AnalysisResult result = analysisService.analyzePhoto(photo, country);
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
            @RequestParam("country") String country) {
        try {
            byte[] bytes = photo.getBytes();

            int faces = faceDetectionService.countFaces(bytes);
            if (faces == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "No face detected. Please upload a clear photo of a person facing the camera.",
                    "errorType", "NO_FACE"
                ));
            }

            byte[] corrected = analysisService.correctPhoto(photo, country);
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
