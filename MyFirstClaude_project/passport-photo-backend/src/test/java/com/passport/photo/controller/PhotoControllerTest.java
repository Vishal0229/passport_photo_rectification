package com.passport.photo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passport.photo.model.AnalysisResult;
import com.passport.photo.model.ComplianceCheck;
import com.passport.photo.model.CountrySpec;
import com.passport.photo.service.FaceDetectionService;
import com.passport.photo.service.PhotoAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PhotoController.class)
class PhotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PhotoAnalysisService analysisService;

    @MockBean
    private FaceDetectionService faceDetectionService;

    private final ObjectMapper json = new ObjectMapper();

    // ─── helpers ─────────────────────────────────────────────────────────────────

    private MockMultipartFile minimalJpeg() throws Exception {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return new MockMultipartFile("photo", "test.jpg", "image/jpeg", baos.toByteArray());
    }

    private CountrySpec usSpec() {
        CountrySpec spec = new CountrySpec();
        spec.setName("United States");
        spec.setWidthPx(600); spec.setHeightPx(600);
        spec.setWidthMm(51); spec.setHeightMm(51);
        spec.setDpi(300);
        spec.setBackgroundColor("WHITE");
        spec.setBackgroundColorHex("#FFFFFF");
        spec.setFaceRatioMin(0.50); spec.setFaceRatioMax(0.69);
        return spec;
    }

    private AnalysisResult dummyResult() {
        List<ComplianceCheck> checks = List.of(
            new ComplianceCheck("Image Dimensions", true, "OK", "≥600×600 px", "600×600 px"));
        return new AnalysisResult("United States", usSpec(), checks, true, 1, 1);
    }

    // ─── GET /api/countries ──────────────────────────────────────────────────────

    @Test
    void getCountries_returns200WithCountryMap() throws Exception {
        when(analysisService.getCountrySpecs()).thenReturn(Map.of("US", usSpec()));

        mockMvc.perform(get("/api/countries"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.US").exists())
            .andExpect(jsonPath("$.US.widthPx").value(600));
    }

    // ─── POST /api/analyze ───────────────────────────────────────────────────────

    @Test
    void analyzePhoto_unsupportedContentType_returns400() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
            "photo", "test.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/analyze")
                .file(pdf)
                .param("country", "US"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Unsupported file type")));
    }

    @Test
    void analyzePhoto_validRequest_returns200WithAnalysis() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.analyzePhoto(any(), eq("US"))).thenReturn(dummyResult());

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.country").value("United States"))
            .andExpect(jsonPath("$.allPassed").value(true))
            .andExpect(jsonPath("$.checks").isArray());
    }

    @Test
    void analyzePhoto_multipleFaces_returns400WithErrorType() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(3);

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorType").value("MULTIPLE_FACES"))
            .andExpect(jsonPath("$.message").value(containsString("3 people detected")));
    }

    @Test
    void analyzePhoto_twoFaces_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(2);

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorType").value("MULTIPLE_FACES"));
    }

    @Test
    void analyzePhoto_unknownCountry_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.analyzePhoto(any(), eq("ZZ")))
            .thenThrow(new IllegalArgumentException("Unknown country code: ZZ"));

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "ZZ"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unknown country code: ZZ"));
    }

    @Test
    void analyzePhoto_serviceThrowsIllegalArgument_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.analyzePhoto(any(), eq("US")))
            .thenThrow(new IllegalArgumentException("Cannot read image"));

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Cannot read image"));
    }

    @Test
    void analyzePhoto_serviceThrowsIOException_returns500() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.analyzePhoto(any(), eq("US")))
            .thenThrow(new java.io.IOException("disk error"));

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value(containsString("Failed to process image")));
    }

    @Test
    void analyzePhoto_withCustomSpec_parsesJsonAndCallsServiceWithSpec() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.analyzePhoto(any(), any(CountrySpec.class))).thenReturn(dummyResult());

        CountrySpec custom = usSpec();
        custom.setName("Custom");

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", json.writeValueAsString(custom)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allPassed").value(true));
    }

    @Test
    void analyzePhoto_malformedCustomSpecJson_returns500() throws Exception {
        // objectMapper.readValue() throws JsonProcessingException (extends IOException) → 500
        when(faceDetectionService.countFaces(any())).thenReturn(0);

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", "{not-valid-json"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value(containsString("Failed to process image")));
    }

    // ─── POST /api/correct ───────────────────────────────────────────────────────

    @Test
    void correctPhoto_validRequest_returnsJpegWithAttachmentHeader() throws Exception {
        byte[] fakeJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.correctPhoto(any(), eq("US"))).thenReturn(fakeJpeg);

        mockMvc.perform(multipart("/api/correct")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andExpect(header().string("Content-Disposition",
                containsString("passport_photo.jpg")));
    }

    @Test
    void correctPhoto_multipleFaces_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(2);

        mockMvc.perform(multipart("/api/correct")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorType").value("MULTIPLE_FACES"));
    }

    @Test
    void correctPhoto_withCustomSpec_returnsJpeg() throws Exception {
        byte[] fakeJpeg = new byte[100];
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.correctPhoto(any(), any(CountrySpec.class))).thenReturn(fakeJpeg);

        mockMvc.perform(multipart("/api/correct")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", json.writeValueAsString(usSpec())))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void correctPhoto_malformedCustomSpecJson_returns500() throws Exception {
        // JsonProcessingException (extends IOException) is thrown inline in the controller → 500
        when(faceDetectionService.countFaces(any())).thenReturn(0);

        mockMvc.perform(multipart("/api/correct")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", "{not-valid-json"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value(containsString("Failed to correct image")));
    }

    @Test
    void correctPhoto_serviceThrowsIllegalArgument_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.correctPhoto(any(), eq("US")))
            .thenThrow(new IllegalArgumentException("Cannot read image"));

        mockMvc.perform(multipart("/api/correct")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Cannot read image"));
    }
}
