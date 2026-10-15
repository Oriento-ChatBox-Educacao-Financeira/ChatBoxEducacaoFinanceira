package com.oriento.api;

import com.oriento.api.config.OrientoAiProperties;
import com.oriento.api.config.OrientoHttpConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import static org.assertj.core.api.Assertions.*;

class OrientoApiApplicationTests {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(OrientoHttpConfig.class)
            .withBean(RestTemplateBuilder.class, RestTemplateBuilder::new)
            .withPropertyValues("oriento.ai.enabled=false", "oriento.ai.base-url=http://127.0.0.1:8000",
                    "oriento.ai.connect-timeout=5s", "oriento.ai.read-timeout=150s", "oriento.ai.internal-token=");

    @Test
    void disabledProxyConfigurationLoadsWithoutExternalServers() {
        context.run(result -> {
            assertThat(result).hasNotFailed();
            assertThat(result.getBean(OrientoAiProperties.class).enabled()).isFalse();
        });
    }

    @Test
    void enabledProxyRequiresInternalToken() {
        context.withPropertyValues("oriento.ai.enabled=true").run(result -> assertThat(result).hasFailed());
    }
}
