package com.shop.ecommerce.controller;

import com.shop.ecommerce.services.ChatContextService;
import com.shop.ecommerce.services.GeminiService;
import com.shop.ecommerce.services.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final GeminiService geminiService;
    private final ChatContextService chatContextService;
    private final JwtService jwtService;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String question = request.getOrDefault("question", "").toString();
        String role = "GUEST";
        Long userId = null;

        // Extract userId and role from JWT token if present (source of truth)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                userId = jwtService.extractUserId(token);
                String tokenRole = jwtService.extractRole(token);
                if (tokenRole != null) {
                    role = tokenRole;
                }
            } catch (Exception e) {
                log.warn("Failed to extract user info from JWT token", e);
            }
        }

        // Fallback to request body if no valid token
        if (userId == null) {
            Object userIdObj = request.get("userId");
            if (userIdObj != null) {
                try {
                    userId = Long.parseLong(userIdObj.toString());
                    role = request.getOrDefault("role", "GUEST").toString();
                } catch (NumberFormatException e) {
                    // userId remains null — treated as guest
                }
            }
        }

        log.debug("Chat request - role: {}, userId: {}", role, userId);

        // Build context from database
        String context = chatContextService.buildContext(question, role, userId);

        // Ask Gemini with context
        String answer = geminiService.ask(question, role, context);

        return ResponseEntity.ok(Map.of("answer", answer));
    }
}
