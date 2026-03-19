package com.shop.ecommerce.dto.order;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Long id;
    private String productName;
    private Long productId;
    private String brandName;
    private Long brandId;
    private String storeName;
    private Long storeId;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
}
