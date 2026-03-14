package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.Brand;
import com.shop.ecommerce.entities.Category;
import com.shop.ecommerce.entities.Product;
import com.shop.ecommerce.entities.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository  extends JpaRepository<Product, Long> {
    List<Product> findByStoreName(String  storeName);
    List<Product> findByBrandName(String brandName);
    List<Product> findByCategoryName(String categoryName);
}
