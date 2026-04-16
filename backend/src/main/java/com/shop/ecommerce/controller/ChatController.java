package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.ChatSqlExecutionDTO;
import com.shop.ecommerce.dto.ChatSqlResultDTO;
import com.shop.ecommerce.services.ChatContextService;
import com.shop.ecommerce.services.ChatSqlService;
import com.shop.ecommerce.services.GeminiService;
import com.shop.ecommerce.services.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.client.RestTemplate;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final GeminiService geminiService;
    private final ChatContextService chatContextService;
    private final ChatSqlService chatSqlService;
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

        // Try Python API First for Analytics
        try {
            RestTemplate restTemplate = new RestTemplate();
            java.util.Map<String, Object> pyReq = java.util.Map.of(
                    "question", question,
                    "role_type", role,
                    "user_id", userId != null ? userId : 0,
                    "jwt_token", authHeader != null ? authHeader.replace("Bearer ", "") : ""
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> pyRes = restTemplate.postForObject(
                    "http://127.0.0.1:8000/api/chat", pyReq, java.util.Map.class);
            
            if (pyRes != null && "IN_SCOPE".equals(pyRes.get("status"))) {
                String pyAnswer = (String) pyRes.get("text");
                String chartJson = (String) pyRes.get("chart_json");
                
                Map<String, String> responseModel = new java.util.HashMap<>();
                responseModel.put("answer", pyAnswer);
                if (chartJson != null) {
                    responseModel.put("chart", chartJson);
                }
                return ResponseEntity.ok(responseModel);
            }
        } catch (Exception e) {
            log.warn("Python AI Service failed or returned error: {}", e.getMessage());
        }

        log.debug("Python AI OUT_OF_SCOPE or failed. Routing to Java Context Gemini Services.");

        // Build context from database for fallback
        String context = chatContextService.buildContext(question, role, userId);

        // Ask Gemini with context
        String answer = geminiService.ask(question, role, context);

        return ResponseEntity.ok(Map.of("answer", answer));
    }

    /**
     * Execute a read-only SQL query from the AI Text2SQL chatbot.
     * Only SELECT queries are allowed. Results are returned as a list of maps.
     */
    @PostMapping("/execute")
    public ResponseEntity<ChatSqlResultDTO> executeSql(
            @RequestBody ChatSqlExecutionDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Extract user info from JWT for audit logging
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                Long tokenUserId = jwtService.extractUserId(token);
                String tokenRole = jwtService.extractRole(token);
                if (tokenUserId != null) {
                    request.setUserId(tokenUserId);
                }
                if (tokenRole != null) {
                    request.setRoleType(tokenRole);
                }
            } catch (Exception e) {
                log.warn("Failed to extract user info from JWT for SQL execution", e);
            }
        }

        log.info("SQL execution request from userId={}, roleType={}", request.getUserId(), request.getRoleType());

        ChatSqlResultDTO result = chatSqlService.executeQuery(request);

        if (result.getError() != null && !result.getError().isEmpty()) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }
}
