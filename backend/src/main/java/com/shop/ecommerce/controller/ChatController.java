package com.shop.ecommerce.controller;

import com.shop.ecommerce.services.ChatContextService;
import com.shop.ecommerce.services.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final GeminiService geminiService;
    private final ChatContextService chatContextService;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(@RequestBody Map<String, Object> request) {
        String question = request.getOrDefault("question", "").toString();
        String role = request.getOrDefault("role", "GUEST").toString();
        Long userId = null;

        Object userIdObj = request.get("userId");
        if (userIdObj != null) {
            try {
                userId = Long.parseLong(userIdObj.toString());
            } catch (NumberFormatException e) {
                // userId remains null — treated as guest
            }
        }

        // Build context from database
        String context = chatContextService.buildContext(question, role, userId);

        // Ask Gemini with context
        String answer = geminiService.ask(question, role, context);

        return ResponseEntity.ok(Map.of("answer", answer));
    }
}
