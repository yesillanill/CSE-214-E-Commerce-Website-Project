package com.shop.ecommerce.dto.user;

import com.shop.ecommerce.enums.Gender;
import com.shop.ecommerce.enums.MembershipType;
import com.shop.ecommerce.enums.Role;
import lombok.Data;
import java.time.LocalDate;
import java.util.Date;

@Data
public class UserProfileDTO {
    // User core fields
    private Long id;
    private String name;
    private String surname;
    private String email;
    private String password;
    private String phone;
    private Role role;
    private Date createdAt;

    // IndividualCustomer fields
    private Gender gender;
    private LocalDate birthdate; // frontend sends birthdate
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private MembershipType membershipType;

    // Store fields
    private String storeName;
    private String companyName;
    private String taxNumber;
    private String taxOffice;
    private String companyAddress;
}
