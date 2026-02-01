package com.shomuran.cardscope.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow these origins
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:8080",
            "https://cardcompass.app",
            "https://www.cardcompass.app",
            "https://cardscope-app.onrender.com",
            "https://cardcompass-web.vercel.app",  // Vercel deployment
            "capacitor://localhost",  // iOS Capacitor app
            "http://localhost",        // Android Capacitor app (HTTP)
            "https://localhost"        // Android/iOS Capacitor app (HTTPS) - REQUIRED
        ));

        // Allow all common HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow all headers
        config.setAllowedHeaders(Arrays.asList("*"));

        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // How long the preflight response can be cached
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
