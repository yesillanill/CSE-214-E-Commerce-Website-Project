package com.shop.ecommerce.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentOrderDTO {
    private Long orderId;
    private LocalDateTime date;
    private int itemCount;
    private BigDecimal total;
    private String status;
}
