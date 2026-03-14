package com.shop.ecommerce.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="stores")
public class Store {

    @Id
    @Column(name="store_id")
    @GeneratedValue
    private Long id;

    @Column(name="owner_id")
    private int ownerId;

    @Column(name="store_name")
    private String name;

    @Column(name="description")
    private String description;
}
