package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.analytics.AdminAnalyticsDTO;
import com.shop.ecommerce.dto.analytics.CorporateAnalyticsDTO;
import com.shop.ecommerce.dto.analytics.IndividualAnalyticsDTO;
import com.shop.ecommerce.services.AnalyticsService;
import com.shop.ecommerce.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final JwtService jwtService;

    @GetMapping("/individual")
    public ResponseEntity<IndividualAnalyticsDTO> getIndividualAnalytics(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(analyticsService.getIndividualAnalytics(userId));
    }

    @GetMapping("/corporate")
    public ResponseEntity<CorporateAnalyticsDTO> getCorporateAnalytics(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(analyticsService.getCorporateAnalytics(userId));
    }

    @GetMapping("/admin")
    public ResponseEntity<AdminAnalyticsDTO> getAdminAnalytics() {
        return ResponseEntity.ok(analyticsService.getAdminAnalytics());
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtService.extractUserId(token);
    }
}
