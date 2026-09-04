package com.inditex.supplier.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the SPA (served from its own origin — {@code http://localhost:5173} in Docker Compose,
 * {@code http://localhost:5173} via the Vite dev server otherwise) to call this API's origin
 * ({@code http://localhost:8080}) from the browser.
 *
 * <p>There is no Spring Security dependency in this project, so without this configuration Spring
 * MVC never emits {@code Access-Control-Allow-Origin} and every cross-origin GET from the frontend
 * is blocked by the browser itself — {@code curl} would still work, which is why this gap wasn't
 * caught by the backend's own (curl-based) verification in SOLUTION.md.
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
