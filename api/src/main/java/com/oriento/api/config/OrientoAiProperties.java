package com.oriento.api.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "oriento.ai")
public record OrientoAiProperties(
        boolean enabled,
        @NotNull URI baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        String internalToken) {

    @AssertTrue(message = "ORIENTO_INTERNAL_TOKEN deve ser configurado quando a IA está habilitada")
    public boolean isInternalTokenConfigured() {
        return !enabled || StringUtils.hasText(internalToken);
    }
}
