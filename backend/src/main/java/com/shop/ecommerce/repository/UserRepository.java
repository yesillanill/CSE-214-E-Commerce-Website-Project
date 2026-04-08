package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Date;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

    long countByRole(com.shop.ecommerce.enums.Role role);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.createdAt >= :since")
    java.util.List<User> findUsersCreatedSince(@org.springframework.data.repository.query.Param("since") Date since);
}
