package com.shop.ecommerce.dto.product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {
    private long id;
    private String name;
    private String description;
    private Double price;
    private String brandName;
    private String categoryName;
    private double rating;
    private String storeName;
    private String img;
    private int soldCount;
    private int stock;
}
