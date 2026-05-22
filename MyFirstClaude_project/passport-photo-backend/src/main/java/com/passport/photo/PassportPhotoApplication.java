package com.passport.photo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Passport Photo Corrector Spring Boot application.
 *
 * <p>Starts the embedded Tomcat server on port 8080 (default). The application
 * exposes REST endpoints under {@code /api} for analyzing and correcting passport
 * photos against country-specific requirements.</p>
 *
 * @version 1.0
 */
@SpringBootApplication
public class PassportPhotoApplication {

    /**
     * Bootstraps the Spring application context and starts the embedded server.
     *
     * @param args command-line arguments passed to the JVM (not used)
     */
    public static void main(String[] args) {
        SpringApplication.run(PassportPhotoApplication.class, args);
    }
}
