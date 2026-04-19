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
import com.shop.ecommerce.config.SqlInjectionValidator;
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
    private final SqlInjectionValidator sqlInjectionValidator;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String question = request.getOrDefault("question", "").toString();

        // SQL injection validation on the chat question
        if (sqlInjectionValidator.containsSqlInjection(question)) {
            log.warn("SQL injection blocked in chat question");
            return ResponseEntity.ok(Map.of("answer",
                    "\uD83D\uDEAB Güvenlik uyarısı: Mesajınızda SQL komutları tespit edildi. " +
                    "SQL sorguları doğrudan yazılamaz. Lütfen sorunuzu doğal dilde ifade edin."));
        }

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
                     role = "GUEST";
                } catch (Exception e) {}
            }
        }

        log.info("Chat API Request | authHeader present: {} | principal class: {} | role: {} | userId: {}",
                (authHeader != null && !authHeader.isBlank()),
                (auth != null ? auth.getPrincipal().getClass().getSimpleName() : "NULL"),
                role, userId);

        log.debug("Chat request - role: {}, userId: {}, corpId: {}", role, userId, corpId);

        // Try Python API First for Analytics
        try {
            var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);   // 5s connect timeout
            factory.setReadTimeout(15000);     // 15s read timeout
            RestTemplate restTemplate = new RestTemplate(factory);
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
            
            if (pyRes != null) {
                String status = (String) pyRes.get("status");
                if ("IN_SCOPE".equals(status)) {
                    String pyAnswer = (String) pyRes.get("text");
                    String chartJson = (String) pyRes.get("chart_json");
                    
                    Map<String, String> responseModel = new java.util.HashMap<>();
                    responseModel.put("answer", pyAnswer);
                    if (chartJson != null) {
                        responseModel.put("chart", chartJson);
                    }
                    return ResponseEntity.ok(responseModel);
                } else if ("ERROR".equals(status)) {
                    log.warn("Python AI returned ERROR. Falling back to Java...");
                    // Python is rate-limited or error'd. We fall back to Java so basic questions work!
                }
            }
        } catch (Exception e) {
            log.warn("Python AI Service failed or returned error: {}", e.getMessage());
        }

        log.debug("Python AI OUT_OF_SCOPE or failed. Routing to Java Context Gemini Services.");

        String context = chatContextService.buildContext(question, role, userId);
        String answer = geminiService.ask(question, role, context);

        // If Gemini API also failed (quota exhausted, 503, etc.), provide a context-based offline response
        if (answer != null && (answer.contains("currently experiencing high demand")
                || answer.contains("currently unavailable")
                || answer.contains("error occurred")
                || answer.contains("API key is not configured"))) {
            log.warn("Java GeminiService also failed. Using offline context-based response.");
            answer = buildOfflineResponse(context, question);
        }

        return ResponseEntity.ok(Map.of("answer", answer));
    }

    /**
     * Build a basic formatted response from the ChatContextService data
     * when ALL AI APIs are unavailable (quota exhausted).
     */
    private String buildOfflineResponse(String context, String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 **İşte veritabanımızdan elde edilen bilgiler:**\n\n");

        // Parse context sections and format them nicely
        String[] sections = context.split("\n\n");
        for (String section : sections) {
            if (section.trim().isEmpty() || section.contains("[NOT LOGGED IN]")
                    || section.contains("=== END")) continue;

            // Clean up section headers
            String cleaned = section
                    .replace("=== ", "**")
                    .replace(" ===", "**")
                    .replace("---", "")
                    .trim();

            if (!cleaned.isEmpty()) {
                sb.append(cleaned).append("\n\n");
            }
        }

        sb.append("\n💡 *Şu anda AI servisi yoğun olduğu için detaylı analiz yapılamamaktadır. ");
        sb.append("Yukarıdaki bilgiler doğrudan veritabanından alınmıştır. ");
        sb.append("Birkaç dakika sonra tekrar denediğinizde daha detaylı yanıtlar alabilirsiniz.*");

        return sb.toString();
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
