package com.shop.ecommerce.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="inventory")
public class Inventory {

    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="store_id")
    private Long storeId;

    @OneToOne
    @JoinColumn(name="product_id")
    @JsonIgnore
    private Product product;

    @Column(name="stock")
    private int stock;
}
