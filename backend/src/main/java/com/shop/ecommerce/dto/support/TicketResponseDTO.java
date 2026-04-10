package com.shop.ecommerce.dto.support;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDTO {
    private Long id;
    private String adminName;
    private String message;
    private LocalDateTime createdAt;
}
