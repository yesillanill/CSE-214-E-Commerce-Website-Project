package com.shop.ecommerce.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoreUpdateDTO {
    private String email;
    private String password;
    private String phone;
    private String storeName;
    private String companyName;
    private String taxNumber;
    private String taxOffice;
    private String componyAddress;
    private String city;
}
