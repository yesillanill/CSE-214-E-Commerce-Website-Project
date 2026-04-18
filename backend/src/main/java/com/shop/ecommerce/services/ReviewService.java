package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.review.ReviewCreateDTO;
import com.shop.ecommerce.dto.review.ReviewDTO;
import com.shop.ecommerce.entities.Product;
import com.shop.ecommerce.entities.Review;
import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.ReviewRepository;
import com.shop.ecommerce.repository.UserRepository;
import com.shop.ecommerce.config.SqlInjectionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SqlInjectionValidator sqlInjectionValidator;

    public List<ReviewDTO> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ReviewDTO createReview(Long userId, ReviewCreateDTO dto) {
        if (dto.getRating() < 1 || dto.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // SQL injection validation on user-submitted text
        sqlInjectionValidator.validate("review comment", dto.getComment());

        reviewRepository.findByUserIdAndProductId(userId, dto.getProductId())
                .ifPresent(r -> {
                    throw new IllegalStateException("You have already reviewed this product");
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        Review saved = reviewRepository.save(review);
        return toDTO(saved);
    }

    public List<ReviewDTO> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalStateException("You can only delete your own reviews");
        }
        reviewRepository.delete(review);
    }

    public double getAverageRating(Long productId) {
        return reviewRepository.averageRatingByProductId(productId);
    }

    public long getReviewCount(Long productId) {
        return reviewRepository.countByProductId(productId);
    }

    private ReviewDTO toDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setProductId(review.getProduct().getId());
        dto.setProductName(review.getProduct().getName());
        dto.setProductImg(review.getProduct().getImg());
        dto.setUserId(review.getUser().getId());
        dto.setUserName(review.getUser().getName() + " " + review.getUser().getSurname());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
