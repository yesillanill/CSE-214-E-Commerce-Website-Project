package com.shop.ecommerce.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shop.ecommerce.enums.PaymentMethod;
import com.shop.ecommerce.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class Payment {

    @Id
    @Column(name = "payment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @Column(name = "payment_method")
    @Convert(converter = com.shop.ecommerce.enums.PaymentMethodConverter.class)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_status")
    @Convert(converter = com.shop.ecommerce.enums.PaymentStatusConverter.class)
    private PaymentStatus paymentStatus;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
