package com.shop.ecommerce.dto.auth;

import com.shop.ecommerce.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndividualRegisterDTO {
    private String name;
    private String surname;
    private String email;
    private String password;
    private String phone;
    private Gender gender;
    private String street;
    private String city;
    private String postalCode;
    private String country;
}
