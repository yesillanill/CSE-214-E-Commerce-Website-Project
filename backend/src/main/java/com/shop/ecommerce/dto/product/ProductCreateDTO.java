package com.shop.ecommerce.dto.product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateDTO {
    private String name;
    private String description;
    private Double price;
    private String brandName;
    private String categoryName;
    private String storeName;
    private String img;
    private int stock;
}
