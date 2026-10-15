package com.oriento.api.controller;

import com.oriento.api.dto.AskResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.services.AIService;
import com.oriento.api.services.AuthenticatedUserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oriento")
public class AIController {
    private final AIService aiService;
    private final AuthenticatedUserService users;

    public AIController(AIService aiService, AuthenticatedUserService users) {
        this.aiService = aiService;
        this.users = users;
    }

    @PostMapping("/ask")
    public AskResponse askAIApi(@RequestBody String prompt,
                               @RequestParam(required = false) String conversationId,
                               @AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = users.resolve(jwt);
        return aiService.askOriento(prompt, conversationId, usuario, jwt.getTokenValue());
    }
}
