package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.ChatSqlExecutionDTO;
import com.shop.ecommerce.dto.ChatSqlResultDTO;
import com.shop.ecommerce.services.ChatContextService;
import com.shop.ecommerce.services.ChatSqlService;
import com.shop.ecommerce.services.GeminiService;
import com.shop.ecommerce.services.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.client.RestTemplate;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    @Value("${ai.chatbot.url:http://127.0.0.1:8000}")
    private String aiChatbotUrl;

    private final GeminiService geminiService;
    private final ChatContextService chatContextService;
    private final ChatSqlService chatSqlService;
    private final JwtService jwtService;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    private final com.shop.ecommerce.repository.UserRepository userRepository;
    private final com.shop.ecommerce.repository.StoreRepository storeRepository;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String question = request.getOrDefault("question", "").toString();
        String role = "GUEST";
        Long userId = null;
        Long corpId = 0L;

        // Extract user identity deeply from DB mapped SecurityContext
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
             String email = ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
             com.shop.ecommerce.entities.User user = userRepository.findByEmail(email).orElse(null);
             if (user != null) {
                 userId = user.getId();
                 role = user.getRole().name();
                 
                 if ("CORPORATE".equals(role)) {
                     com.shop.ecommerce.entities.Store store = storeRepository.findByOwner(user).orElse(null);
                     if (store != null) {
                         corpId = store.getId();
                     }
                 }
             }
        } else if (userId == null) {
            // Fallback for unauthenticated access (Guest)
            Object userIdObj = request.get("userId");
            if (userIdObj != null) {
                try {
                     // userId remains null internally to prevent spoofing a user id without token
                     role = "GUEST";
                } catch (Exception e) {}
            }
        }

        log.debug("Chat request - role: {}, userId: {}, corpId: {}", role, userId, corpId);

        // Try Python API First for Analytics
        try {
            RestTemplate restTemplate = new RestTemplate();
            java.util.Map<String, Object> pyReq = java.util.Map.of(
                    "question", question,
                    "role_type", role,
                    "user_id", userId != null ? userId : 0,
                    "corp_id", corpId,
                    "jwt_token", authHeader != null ? authHeader.replace("Bearer ", "") : ""
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> pyRes = restTemplate.postForObject(
                    aiChatbotUrl + "/api/chat", pyReq, java.util.Map.class);
            
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

        String context = chatContextService.buildContext(question, role, userId);
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

        // Validate execution safety dynamically via SecurityContext (DB truth)
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
             String email = ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
             com.shop.ecommerce.entities.User user = userRepository.findByEmail(email).orElse(null);
             if (user != null) {
                 request.setUserId(user.getId());
                 request.setRoleType(user.getRole().name());
                 request.setCorpId(0L);
                 
                 if ("CORPORATE".equals(user.getRole().name())) {
                     com.shop.ecommerce.entities.Store store = storeRepository.findByOwner(user).orElse(null);
                     if (store != null) {
                         request.setCorpId(store.getId());
                     }
                 }
             }
        } else {
             // Unauthenticated execution from Python AI (Guest mode)
             request.setUserId(0L);
             request.setRoleType("GUEST");
             request.setCorpId(0L);
        }

        log.info("SQL execution request from userId={}, corpId={}, roleType={}", request.getUserId(), request.getCorpId(), request.getRoleType());

        ChatSqlResultDTO result = chatSqlService.executeQuery(request);

        if (result.getError() != null && !result.getError().isEmpty()) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }
}
