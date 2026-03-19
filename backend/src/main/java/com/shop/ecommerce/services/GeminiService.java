package com.shop.ecommerce.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static final Map<String, String> SYSTEM_PROMPTS = Map.of(
            "INDIVIDUAL",
            "You are a helpful e-commerce assistant. You can only answer questions about the authenticated user's own orders, purchases, reviews, and spending patterns. Never reveal data belonging to other users.",
            "CORPORATE",
            "You are a store analytics assistant. You can only answer questions about the authenticated corporate user's own store: products, orders, customers, reviews, and sales data. Never reveal data belonging to other stores.",
            "ADMIN",
            "You are a platform-wide analytics assistant with full access to all stores, users, and aggregate data."
    );

    public String ask(String question, String role) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Gemini API key is not configured. Please set the GEMINI_API_KEY environment variable.";
        }

        String systemPrompt = SYSTEM_PROMPTS.getOrDefault(role, SYSTEM_PROMPTS.get("INDIVIDUAL"));

        try {
            RestTemplate restTemplate = new RestTemplate();

            String url = GEMINI_URL + "?key=" + apiKey;

            Map<String, Object> systemPart = Map.of("text", systemPrompt);
            Map<String, Object> systemContent = Map.of("role", "user", "parts", List.of(systemPart));

            Map<String, Object> userPart = Map.of("text", question);
            Map<String, Object> userContent = Map.of("role", "user", "parts", List.of(userPart));

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("role", "user", "parts", List.of(
                                    Map.of("text", systemPrompt + "\n\nUser question: " + question)
                            ))
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }

            return "I couldn't generate a response. Please try again.";
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            return "An error occurred while contacting the AI service: " + e.getMessage();
        }
    }
}
