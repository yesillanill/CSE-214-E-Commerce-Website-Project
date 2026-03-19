package com.shop.ecommerce.entities;

import com.shop.ecommerce.enums.Gender;
import com.shop.ecommerce.enums.MembershipType;
import com.shop.ecommerce.enums.SatisfactionLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "individual_customers")
public class IndividualCustomer {

    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "street")
    private String street;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country;

    @Column(name = "membership_type")
    @Enumerated(EnumType.STRING)
    private MembershipType membershipType;

    @Column(name = "total_spend", precision = 15, scale = 2)
    private BigDecimal totalSpend = BigDecimal.ZERO;

    @Column(name = "items_purchased")
    private Integer itemsPurchased;

    @Column(name = "avg_rating")
    private Double avgRating;

    @Column(name = "discount_applied")
    private Boolean discountApplied;

    @Column(name = "satisfaction_level")
    @Enumerated(EnumType.STRING)
    private SatisfactionLevel satisfactionLevel;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
}
