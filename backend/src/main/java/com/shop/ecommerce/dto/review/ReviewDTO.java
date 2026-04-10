package com.shop.ecommerce.dto.review;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImg;
    private Long userId;
    private String userName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
