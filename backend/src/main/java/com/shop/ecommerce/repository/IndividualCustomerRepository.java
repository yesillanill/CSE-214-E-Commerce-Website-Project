package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.IndividualCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shop.ecommerce.entities.User;
import java.util.Optional;

@Repository
public interface IndividualCustomerRepository extends JpaRepository<IndividualCustomer, Long> {
    Optional<IndividualCustomer> findByUser(User user);
}
