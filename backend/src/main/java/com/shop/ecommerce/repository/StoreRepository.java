package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shop.ecommerce.entities.User;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByOwner(User owner);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Store s WHERE s.totalRevenue > 0")
    long countActiveStores();
}
