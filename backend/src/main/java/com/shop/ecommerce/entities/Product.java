package com.shop.ecommerce.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="products")
public class Product {

    @Id
    @Column(name="product_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name="name", nullable = false)
    private String name;

    @Column(name="description", nullable = true)
    private String description;

    @Column(name="price", nullable = false)
    private Double price;

    @ManyToOne
    @JoinColumn(name="brand_id")
    private Brand brand;

    @ManyToOne
    @JoinColumn(name="category_id")
    private Category category;

    @Column(name="rating")
    private double rating;

    @ManyToOne
    @JoinColumn(name="store_id")
    private Store store;

    @Column(name="image_url")
    private String img;

    @Column(name="sold_count")
    private int soldCount;

    @CreationTimestamp
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "product")
    private Inventory inventory;
}
