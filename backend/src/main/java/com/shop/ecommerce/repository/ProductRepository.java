package com.shop.ecommerce.repository;
import com.shop.ecommerce.dto.product.ProductListDTO;
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

    @Query("SELECT new com.shop.ecommerce.dto.product.ProductListDTO(" +
           "p.id, p.name, CAST(p.price AS double), " +
           "COALESCE(AVG(r.rating), 0.0), COUNT(r), " +
           "p.img, p.category.name, p.brand.name, p.inventory.stock) " +
           "FROM Product p " +
           "LEFT JOIN Review r ON r.product = p " +
           "GROUP BY p.id, p.name, p.price, p.img, p.category.name, p.brand.name, p.inventory.stock")
    List<ProductListDTO> findAllWithRatings();

    @Query("SELECT new com.shop.ecommerce.dto.product.ProductListDTO(" +
           "p.id, p.name, CAST(p.price AS double), " +
           "COALESCE(AVG(r.rating), 0.0), COUNT(r), " +
           "p.img, p.category.name, p.brand.name, p.inventory.stock) " +
           "FROM Product p " +
           "LEFT JOIN Review r ON r.product = p " +
           "WHERE p.category.name = :categoryName " +
           "GROUP BY p.id, p.name, p.price, p.img, p.category.name, p.brand.name, p.inventory.stock")
    List<ProductListDTO> findByCategoryWithRatings(@Param("categoryName") String categoryName);

    @Query("SELECT new com.shop.ecommerce.dto.product.ProductListDTO(" +
           "p.id, p.name, CAST(p.price AS double), " +
           "COALESCE(AVG(r.rating), 0.0), COUNT(r), " +
           "p.img, p.category.name, p.brand.name, p.inventory.stock) " +
           "FROM Product p " +
           "LEFT JOIN Review r ON r.product = p " +
           "WHERE p.brand.name = :brandName " +
           "GROUP BY p.id, p.name, p.price, p.img, p.category.name, p.brand.name, p.inventory.stock")
    List<ProductListDTO> findByBrandWithRatings(@Param("brandName") String brandName);

    @Query("SELECT new com.shop.ecommerce.dto.product.ProductListDTO(" +
           "p.id, p.name, CAST(p.price AS double), " +
           "COALESCE(AVG(r.rating), 0.0), COUNT(r), " +
           "p.img, p.category.name, p.brand.name, p.inventory.stock) " +
           "FROM Product p " +
           "LEFT JOIN Review r ON r.product = p " +
           "WHERE p.store.storeName = :storeName " +
           "GROUP BY p.id, p.name, p.price, p.img, p.category.name, p.brand.name, p.inventory.stock")
    List<ProductListDTO> findByStoreWithRatings(@Param("storeName") String storeName);

    @Query("SELECT new com.shop.ecommerce.dto.product.ProductListDTO(" +
           "p.id, p.name, CAST(p.price AS double), " +
           "COALESCE(AVG(r.rating), 0.0), COUNT(r), " +
           "p.img, p.category.name, p.brand.name, p.inventory.stock) " +
           "FROM Product p " +
           "LEFT JOIN Review r ON r.product = p " +
           "GROUP BY p.id, p.name, p.price, p.img, p.category.name, p.brand.name, p.inventory.stock " +
           "ORDER BY COALESCE(AVG(r.rating), 0.0) DESC")
    List<ProductListDTO> findTopRatedWithRatings(Pageable pageable);

    Page<Product> findByStoreIdAndNameContainingIgnoreCase(Long storeId, String name, Pageable pageable);
}
