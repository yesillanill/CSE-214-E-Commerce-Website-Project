package com.shop.ecommerce.entities;

import com.shop.ecommerce.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="users")
public class User {

    @Id
    @Column(name="user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="name")
    private String name;

    @Column(name="surname")
    private String surname;

    @Column(name="email")
    private String email;

    @Column(name="password")
    private String password;

    @Column(name="phone")
    private String phone;

    @Column(name="role")
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name="created_at")
    private Date createdAt;
}
