package com.shop.ecommerce.dto.product;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListDTO {
    private Long id;
    private String name;
    private Double price;
    private double rating;
    private long reviewCount;
    private String img;
    private String categoryName;
    private String brandName;
    private Integer stock;
}
