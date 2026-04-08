package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.analytics.PublicStatsDTO;
import com.shop.ecommerce.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicStatsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/stats")
    public ResponseEntity<PublicStatsDTO> getPublicStats() {
        return ResponseEntity.ok(analyticsService.getPublicStats());
    }
}
