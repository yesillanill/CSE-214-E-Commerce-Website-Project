package com.shop.ecommerce.dto.order;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private LocalDateTime createdAt;
    private BigDecimal grandTotal;
    private String shippingAddress;

    // Payment
    private String paymentStatus;
    private String paymentMethod;

    // Shipment
    private String shippingMethod;
    private BigDecimal shippingCost;
    private LocalDate shipmentDate;
    private String shipmentStatus;
    private LocalDate deliveryDate;
    private LocalDate estimatedDelivery;
    private String trackingNumber;

    private List<OrderItemDTO> orderItems;
}
