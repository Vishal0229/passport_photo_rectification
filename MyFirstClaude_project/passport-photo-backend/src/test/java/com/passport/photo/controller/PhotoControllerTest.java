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
            .andExpect(header().string("X-Privacy-Notice", "Photo data is processed in-memory only and is not stored on disk."))
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
            .andExpect(jsonPath("$.message").value(containsString("Image processing failed")));
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
            .andExpect(jsonPath("$.message").value(containsString("Image processing failed")));
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
            .andExpect(header().string("X-Privacy-Notice", "Photo data is processed in-memory only and is not stored on disk."))
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
            .andExpect(jsonPath("$.message").value(containsString("Image processing failed")));
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

    // ─── POST /api/pdf ───────────────────────────────────────────────────────────

    @Test
    void photoPdf_validRequest_returnsPdfWithAttachmentHeader() throws Exception {
        byte[] fakePdf = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.generatePhotoPdf(any(), eq("US"))).thenReturn(fakePdf);

        mockMvc.perform(multipart("/api/pdf")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Privacy-Notice", "Photo data is processed in-memory only and is not stored on disk."))
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition",
                containsString("passport_photo_sheet.pdf")));
    }

    @Test
    void photoPdf_multipleFaces_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(2);

        mockMvc.perform(multipart("/api/pdf")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorType").value("MULTIPLE_FACES"));
    }

    @Test
    void photoPdf_withCustomSpec_returnsPdf() throws Exception {
        byte[] fakePdf = new byte[100];
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.generatePhotoPdf(any(), any(CountrySpec.class))).thenReturn(fakePdf);

        mockMvc.perform(multipart("/api/pdf")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", json.writeValueAsString(usSpec())))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void photoPdf_malformedCustomSpecJson_returns500() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);

        mockMvc.perform(multipart("/api/pdf")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", "{not-valid-json"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value(containsString("Image processing failed")));
    }

    @Test
    void photoPdf_serviceThrowsIllegalArgument_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.generatePhotoPdf(any(), eq("US")))
            .thenThrow(new IllegalArgumentException("Cannot read image"));

        mockMvc.perform(multipart("/api/pdf")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Cannot read image"));
    }

    // ─── POST /api/sheet ─────────────────────────────────────────────────────────

    @Test
    void photoSheet_validRequest_returnsJpegWithAttachmentHeader() throws Exception {
        byte[] fakeJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.generatePhotoSheet(any(), eq("US"))).thenReturn(fakeJpeg);

        mockMvc.perform(multipart("/api/sheet")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Privacy-Notice", "Photo data is processed in-memory only and is not stored on disk."))
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andExpect(header().string("Content-Disposition",
                containsString("passport_photo_sheet.jpg")));
    }

    @Test
    void photoSheet_multipleFaces_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(2);

        mockMvc.perform(multipart("/api/sheet")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorType").value("MULTIPLE_FACES"));
    }

    @Test
    void photoSheet_withCustomSpec_returnsJpeg() throws Exception {
        byte[] fakeJpeg = new byte[100];
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.generatePhotoSheet(any(), any(CountrySpec.class))).thenReturn(fakeJpeg);

        mockMvc.perform(multipart("/api/sheet")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", json.writeValueAsString(usSpec())))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void photoSheet_malformedCustomSpecJson_returns500() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);

        mockMvc.perform(multipart("/api/sheet")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", "{not-valid-json"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value(containsString("Image processing failed")));
    }

    @Test
    void photoSheet_serviceThrowsIllegalArgument_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        when(analysisService.generatePhotoSheet(any(), eq("US")))
            .thenThrow(new IllegalArgumentException("Cannot read image"));

        mockMvc.perform(multipart("/api/sheet")
                .file(minimalJpeg())
                .param("country", "US"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Cannot read image"));
    }

    // ─── validateCustomSpecInput — country=Custom without body ───────────────────

    @Test
    void analyzePhoto_customCountryWithoutSpec_returns400() throws Exception {
        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "Custom"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("customSpec JSON body is required")));
    }

    @Test
    void correctPhoto_customCountryWithoutSpec_returns400() throws Exception {
        mockMvc.perform(multipart("/api/correct")
                .file(minimalJpeg())
                .param("country", "Custom"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("customSpec JSON body is required")));
    }

    // ─── validateCustomSpecInput — oversized JSON ────────────────────────────────

    @Test
    void analyzePhoto_oversizedCustomSpecJson_returns400() throws Exception {
        String hugeJson = "{\"name\":\"X\",\"pad\":\"" + "A".repeat(4100) + "\"}";

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", hugeJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("must not exceed 4096 characters")));
    }

    // ─── validateCustomSpec — invalid dimensions ─────────────────────────────────

    @Test
    void analyzePhoto_customSpecZeroWidth_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        CountrySpec bad = usSpec(); bad.setWidthPx(0);

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", json.writeValueAsString(bad)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("widthPx and heightPx must be greater than 0")));
    }

    @Test
    void analyzePhoto_customSpecNegativeHeight_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        CountrySpec bad = usSpec(); bad.setHeightPx(-1);

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", json.writeValueAsString(bad)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("widthPx and heightPx must be greater than 0")));
    }

    @Test
    void analyzePhoto_customSpecWidthExceedsMax_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        CountrySpec bad = usSpec(); bad.setWidthPx(3001);

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", json.writeValueAsString(bad)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("cannot exceed 3000")));
    }

    @Test
    void analyzePhoto_customSpecHeightExceedsMax_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        CountrySpec bad = usSpec(); bad.setHeightPx(3001);

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", json.writeValueAsString(bad)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("cannot exceed 3000")));
    }

    // ─── validateCustomSpec — invalid backgroundColorHex ────────────────────────

    @Test
    void analyzePhoto_customSpecNullHex_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        CountrySpec bad = usSpec(); bad.setBackgroundColorHex(null);

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", json.writeValueAsString(bad)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("backgroundColorHex must be a valid 6-digit hex")));
    }

    @Test
    void analyzePhoto_customSpecInvalidHexFormat_returns400() throws Exception {
        when(faceDetectionService.countFaces(any())).thenReturn(0);
        CountrySpec bad = usSpec(); bad.setBackgroundColorHex("white");

        mockMvc.perform(multipart("/api/analyze")
                .file(minimalJpeg())
                .param("country", "Custom")
                .param("customSpec", json.writeValueAsString(bad)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("backgroundColorHex must be a valid 6-digit hex")));
    }

    // ─── POST /api/log ───────────────────────────────────────────────────────────

    @Test
    void clientLog_infoLevel_returns204() throws Exception {
        mockMvc.perform(post("/api/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":\"info\",\"message\":\"test msg\",\"context\":\"{}\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void clientLog_warnLevel_returns204() throws Exception {
        mockMvc.perform(post("/api/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":\"warn\",\"message\":\"something odd\",\"context\":\"\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void clientLog_errorLevel_returns204() throws Exception {
        mockMvc.perform(post("/api/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":\"error\",\"message\":\"crash\",\"context\":\"stack trace\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void clientLog_unknownLevel_defaultsToErrorAndReturns204() throws Exception {
        mockMvc.perform(post("/api/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":\"verbose\",\"message\":\"debug info\",\"context\":\"\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void clientLog_longMessageTruncated_returns204() throws Exception {
        String longMessage  = "M".repeat(600);
        String longContext  = "C".repeat(1100);
        String body = json.writeValueAsString(
            Map.of("level", "error", "message", longMessage, "context", longContext));

        mockMvc.perform(post("/api/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNoContent());
    }

    @Test
    void clientLog_missingFields_usesDefaults_returns204() throws Exception {
        mockMvc.perform(post("/api/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isNoContent());
    }
}
