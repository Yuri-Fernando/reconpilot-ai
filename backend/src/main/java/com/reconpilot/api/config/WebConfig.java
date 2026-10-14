package com.reconpilot.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String[] frontendOrigins;

    public WebConfig(@Value("${app.frontend-origins:${app.frontend-origin:http://localhost:4200}}") String frontendOrigins) {
        this.frontendOrigins = Arrays.stream(frontendOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(frontendOrigins)
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type", "X-Correlation-Id");
    }
}
