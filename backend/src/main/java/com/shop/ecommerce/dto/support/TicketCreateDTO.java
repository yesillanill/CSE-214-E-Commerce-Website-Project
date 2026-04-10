package com.shop.ecommerce.dto.support;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreateDTO {
    private String subject;
    private String message;
    private String type;
    private Long productId;
    private Long reviewId;
}
