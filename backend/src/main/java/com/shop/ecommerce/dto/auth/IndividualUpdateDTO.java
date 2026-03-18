package com.shop.ecommerce.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndividualUpdateDTO {
    private String email;
    private String password;
    private String phone;
    private String street;
    private String city;
    private String postalCode;
    private String country;
}
