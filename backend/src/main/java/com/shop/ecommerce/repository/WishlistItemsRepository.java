package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.WishlistItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemsRepository extends JpaRepository<WishlistItems, Long> {
    List<WishlistItems> findByWishlistId(Long wishlistId);
    Optional<WishlistItems> findByWishlistIdAndProductId(Long wishlistId, Long productId);
    void deleteByWishlistId(Long wishlistId);
}
