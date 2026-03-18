package com.shop.ecommerce.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="cart_id")
    private Long cartId;

    @Column(name="product_id")
    private Long productId;

    @Column(name="quantity")
    private Integer quantity;

    @OneToOne
    @JoinColumn(name="cart_id")
    private Cart cart;

    @OneToOne
    @JoinColumn(name="product_id")
    private Product product;
}
