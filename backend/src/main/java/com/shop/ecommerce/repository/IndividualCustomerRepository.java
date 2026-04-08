package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.IndividualCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shop.ecommerce.entities.User;
import java.util.Optional;

@Repository
public interface IndividualCustomerRepository extends JpaRepository<IndividualCustomer, Long> {
    Optional<IndividualCustomer> findByUser(User user);

    @org.springframework.data.jpa.repository.Query("SELECT ic.membershipType, COUNT(ic) FROM IndividualCustomer ic GROUP BY ic.membershipType")
    java.util.List<Object[]> countByMembershipType();

    @org.springframework.data.jpa.repository.Query("SELECT ic.satisfactionLevel, COUNT(ic) FROM IndividualCustomer ic WHERE ic.satisfactionLevel IS NOT NULL GROUP BY ic.satisfactionLevel")
    java.util.List<Object[]> countBySatisfactionLevel();
}
