package com.shop.ecommerce.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shop.ecommerce.enums.ShipmentStatus;
import com.shop.ecommerce.enums.ShippingMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipments")
public class Shipment {

    @Id
    @Column(name = "shipment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @Column(name = "shipping_method")
    @Convert(converter = com.shop.ecommerce.enums.ShippingMethodConverter.class)
    private ShippingMethod shippingMethod;

    @Column(name = "shipping_cost", precision = 10, scale = 2)
    private BigDecimal shippingCost;

    @Column(name = "status")
    @Convert(converter = com.shop.ecommerce.enums.ShipmentStatusConverter.class)
    private ShipmentStatus status;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "shipment_date")
    private LocalDate shipmentDate;

    @Column(name = "estimated_delivery")
    private LocalDate estimatedDelivery;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;
}
