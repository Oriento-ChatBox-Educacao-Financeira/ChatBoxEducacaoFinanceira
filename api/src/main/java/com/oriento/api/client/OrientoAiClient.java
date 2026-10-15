package com.oriento.api.client;

import com.oriento.api.config.OrientoAiProperties;
import com.oriento.api.dto.AskResponse;
import com.oriento.api.dto.ai.OrientoAskRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OrientoAiClient {
    private final RestTemplate restTemplate;
    private final OrientoAiProperties properties;

    public OrientoAiClient(@Qualifier("orientoRestTemplate") RestTemplate restTemplate,
                           OrientoAiProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public AskResponse ask(OrientoAskRequest request, String userToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(userToken);
        headers.set("X-Internal-Token", properties.internalToken());
        String url = properties.baseUrl().toString().replaceAll("/+$", "") + "/internal/oriento/ask";
        try {
            AskResponse response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(request, headers), AskResponse.class).getBody();
            if (response == null || !StringUtils.hasText(response.response())
                    || !request.conversationId().toString().equals(response.conversationId())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta inválida do serviço de IA");
            }
            return response;
        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            if (status == 401 || status == 403 || status == 404) {
                throw new ResponseStatusException(HttpStatus.valueOf(status), "Acesso ao serviço de IA recusado");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de IA indisponível");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de IA indisponível");
        }
    }
}
