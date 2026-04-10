package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p WHERE p.store.storeName = :storeName")
    List<Product> findByStoreName(@Param("storeName") String storeName);

    List<Product> findByBrandName(String brandName);

    List<Product> findByCategoryName(String categoryName);

    Page<Product> findByStoreId(Long storeId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    long countByStoreId(Long storeId);

    List<Product> findAllByOrderBySoldCountDesc(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT p.brand) FROM Product p")
    long countDistinctBrands();

    List<Product> findByStoreId(Long storeId);
}
