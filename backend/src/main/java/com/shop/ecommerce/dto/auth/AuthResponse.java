package com.shop.ecommerce.dto.auth;

import com.shop.ecommerce.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long id;
    private String name;
    private String surname;
    private String email;
    private String phone;
    private Role role;
}
