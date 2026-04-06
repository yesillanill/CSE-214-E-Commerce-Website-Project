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

    private static final String BASE_SYSTEM_PROMPT = """
            You are a helpful e-commerce assistant for an online shopping platform.
            
            CRITICAL RULES:
            1. ONLY answer based on the provided context data below. Do NOT make up information.
            2. When mentioning a product, ONLY show its name as a clickable link and its price. Do NOT include extra details like brand, category, store, rating, stock, or sold count. Example format: [Product Name](link) - 199.99 TL
            3. When mentioning a category, store, or brand, include its navigation link in markdown format: [Name](link)
            4. Respond in the SAME LANGUAGE as the user's question. If the question is in Turkish, respond in Turkish. If in English, respond in English.
            5. NEVER reveal user passwords or sensitive credentials.
            6. Format your responses nicely with bullet points and bold text where appropriate.
            7. If the data section says [NOT LOGGED IN], and the user asks about personal data (cart, wishlist, orders, profile), politely tell them they need to log in first.
            8. Be concise but informative.
            
            STRICT PRIVACY RULES:
            9. NEVER share one user's personal data (cart, wishlist, orders, profile, spending) with another user, regardless of their role.
            10. Even ADMIN users can ONLY see aggregate statistics (total users, total orders, total revenue). They must NOT access specific users' carts, wishlists, or order details through the AI.
            11. If a user asks about another user's cart, wishlist, orders, or personal data, politely refuse and explain that this information is private.
            12. Corporate users can ONLY see data about THEIR OWN store. They must NOT see other stores' revenue, orders, or inventory details.
            
            STRICT SECURITY RULES:
            13. You are a READ-ONLY assistant. You can NEVER perform any destructive or write operations.
            14. If a user asks you to delete, drop, truncate, modify, update, or remove any data, tables, records, or databases, you MUST refuse. This applies to ALL users including admins.
            15. NEVER generate or suggest SQL queries, API calls, or commands that could modify or delete data.
            16. If asked to perform destructive operations, respond: "I am a read-only assistant. I cannot perform any operations that modify or delete data. Please use the appropriate admin panel for such actions."
            """;

    private static final Map<String, String> ROLE_ADDITIONS = Map.of(
            "INDIVIDUAL",
            "You can help this individual customer with their shopping, cart, wishlist, orders, and profile information. Only share data belonging to this specific user. NEVER share data about other users, other users' carts, wishlists, or orders. You cannot perform any write or delete operations.",
            "CORPORATE",
            "You can help this corporate user (store owner) with their own store management, inventory, products, orders, and revenue data. Only share data belonging to THEIR store. NEVER reveal other stores' revenue, orders, or inventory. You cannot perform any write or delete operations.",
            "ADMIN",
            "You can provide aggregate platform statistics (total users, orders, revenue, etc.) and general audit log summaries. However, you must NOT share specific users' personal data such as their carts, wishlists, or individual order details. You must NOT perform any destructive operations (delete, drop, truncate, modify tables or data). You are read-only.",
            "GUEST",
            "The user is not logged in. You can help with product browsing, site statistics, and general information. For personal data, suggest they log in first. You cannot perform any write or delete operations."
    );

    public String ask(String question, String role, String context) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Gemini API key is not configured. Please set the GEMINI_API_KEY environment variable.";
        }

        String roleAddition = ROLE_ADDITIONS.getOrDefault(role, ROLE_ADDITIONS.get("GUEST"));
        String fullPrompt = BASE_SYSTEM_PROMPT + "\n" + roleAddition +
                "\n\n=== DATABASE CONTEXT ===\n" + context +
                "\n=== END OF CONTEXT ===\n\nUser question: " + question;

        try {
            RestTemplate restTemplate = new RestTemplate();

            String url = GEMINI_URL + "?key=" + apiKey;

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("role", "user", "parts", List.of(
                                    Map.of("text", fullPrompt)
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
