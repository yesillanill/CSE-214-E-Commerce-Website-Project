package com.shop.ecommerce.mapper;

import com.shop.ecommerce.dto.product.*;
import com.shop.ecommerce.entities.Product;

public class ProductMapper {

    public static ProductListDTO toListDTO(Product product){
        ProductListDTO dto = new ProductListDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setRating(product.getRating());
        dto.setImg(product.getImg());
        dto.setStock(product.getInventory() != null ? product.getInventory().getStock() : 0);
        return dto;
    }

    public static ProductDetailDTO toDetailDTO(Product product){
        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setRating(product.getRating());
        dto.setBrandName(product.getBrand().getName());
        dto.setStoreName(product.getStore().getStoreName());
        dto.setCategoryName(product.getCategory().getName());
        dto.setStock(product.getInventory().getStock());
        dto.setImg(product.getImg());
        dto.setSoldCount(product.getSoldCount());
        return dto;
    }

    public static ProductCreateDTO toCreateDTO(Product product){
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setBrandName(product.getBrand().getName());
        dto.setStoreName(product.getStore().getStoreName());
        dto.setCategoryName(product.getCategory().getName());
        dto.setStock(product.getInventory().getStock());
        dto.setImg(product.getImg());
        return dto;
    }

    public static ProductUpdateDTO toUpdateDTO(Product product){
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getInventory().getStock());
        dto.setImg(product.getImg());
        return dto;
    }

    public static Product toEntity(ProductCreateDTO dto){
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setImg(dto.getImg());
        return product;
    }
}
