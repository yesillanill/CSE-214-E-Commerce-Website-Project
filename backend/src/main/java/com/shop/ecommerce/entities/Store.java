package com.shop.ecommerce.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="stores")
public class Store {

    @Id
    @Column(name="store_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="store_name")
    private String storeName;

    @Column(name="description")
    private String description;

    @Column(name="company_name")
    private String companyName;

    @Column(name="tax_number")
    private String taxNumber;

    @Column(name="tax_office")
    private String taxOffice;

    @Column(name="company_address")
    private String componyAddress;

    @OneToOne
    @JoinColumn(name="owner_id")
    private User owner;
}
