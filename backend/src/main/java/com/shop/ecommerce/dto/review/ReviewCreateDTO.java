package com.shop.ecommerce.dto.review;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateDTO {
    private Long productId;
    private int rating;
    private String comment;
}
