package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.review.ReviewCreateDTO;
import com.shop.ecommerce.dto.review.ReviewDTO;
import com.shop.ecommerce.services.JwtService;
import com.shop.ecommerce.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtService jwtService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDTO>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    @GetMapping("/user")
    public ResponseEntity<List<ReviewDTO>> getUserReviews(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(reviewService.getReviewsByUser(userId));
    }

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ReviewCreateDTO dto) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(reviewService.createReview(userId, dto));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long reviewId) {
        Long userId = extractUserId(authHeader);
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtService.extractUserId(token);
    }
}
