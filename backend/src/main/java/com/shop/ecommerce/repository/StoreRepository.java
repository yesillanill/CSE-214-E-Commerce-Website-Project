package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shop.ecommerce.entities.User;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByOwner(User owner);

    @Query("SELECT COUNT(s) FROM Store s WHERE s.totalRevenue > 0")
    long countActiveStores();

    Page<Store> findByStoreNameContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(String storeName, String companyName, Pageable pageable);
    
    @Query("SELECT s FROM Store s WHERE " +
           "LOWER(s.storeName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.taxNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Store> searchStores(@Param("searchTerm") String searchTerm, Pageable pageable);
}
