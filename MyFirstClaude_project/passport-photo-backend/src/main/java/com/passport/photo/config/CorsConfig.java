package com.passport.photo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures Cross-Origin Resource Sharing (CORS) for the REST API.
 *
 * <p>Restricts browser access to requests originating from the Vite dev server
 * at {@code http://localhost:3000}. For production, update {@code allowedOrigins}
 * to match the deployed frontend URL.</p>
 *
 * @version 1.0
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * Registers CORS rules for all {@code /api/**} routes.
     *
     * <p>Allows GET, POST, and preflight OPTIONS requests from the local
     * frontend origin. The preflight response is cached by the browser for
     * 3600 seconds (1 hour).</p>
     *
     * @param registry the Spring MVC CORS registry to add mappings to
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
