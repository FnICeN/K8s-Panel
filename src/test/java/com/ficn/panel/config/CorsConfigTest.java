package com.ficn.panel.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void preflightAllowsPatchFromFrontend() {
        InspectableCorsRegistry registry = new InspectableCorsRegistry();
        new CorsConfig().addCorsMappings(registry);

        var configuration = registry.configurations().get("/**");
        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedMethods()).contains("PATCH", "OPTIONS");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    private static class InspectableCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }
}
