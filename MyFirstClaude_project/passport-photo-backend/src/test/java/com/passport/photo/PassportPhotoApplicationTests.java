package com.passport.photo;

import com.passport.photo.service.FaceDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class PassportPhotoApplicationTests {

    @MockBean
    private FaceDetectionService faceDetectionService;

    @Test
    void contextLoads() {}
}
