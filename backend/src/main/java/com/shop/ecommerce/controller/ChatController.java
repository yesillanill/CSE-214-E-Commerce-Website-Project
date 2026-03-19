package com.shop.ecommerce.controller;

import com.shop.ecommerce.services.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ChatController {

    private final GeminiService geminiService;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(@RequestBody Map<String, String> request) {
        String question = request.getOrDefault("question", "");
        String role = request.getOrDefault("role", "INDIVIDUAL");

        String answer = geminiService.ask(question, role);
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}
