package com.shop.ecommerce.dto.support;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String subject;
    private String message;
    private String type;
    private String status;
    private Long productId;
    private String productName;
    private Long reviewId;
    private LocalDateTime createdAt;
    private List<TicketResponseDTO> responses;
}
