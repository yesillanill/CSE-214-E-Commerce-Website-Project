package com.shop.ecommerce.mapper;

import com.shop.ecommerce.dto.auth.*;
import com.shop.ecommerce.entities.IndividualCustomer;
import com.shop.ecommerce.entities.Store;
import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.enums.Role;

import java.util.Date;

public class AuthMapper {

    public static User toUserEntityFromIndividual(IndividualRegisterDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setPhone(dto.getPhone());
        user.setRole(Role.INDIVIDUAL);
        user.setCreatedAt(new Date());
        return user;
    }

    public static IndividualCustomer toIndividualCustomerEntity(IndividualRegisterDTO dto, User user) {
        IndividualCustomer customer = new IndividualCustomer();
        customer.setGender(dto.getGender());
        customer.setBirthDate(dto.getBirthDate());
        customer.setStreet(dto.getStreet());
        customer.setPostalCode(dto.getPostalCode());
        customer.setCountry(dto.getCountry());
        customer.setUser(user);
        return customer;
    }

    public static User toUserEntityFromStore(StoreRegisterDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setPhone(dto.getPhone());
        user.setRole(Role.CORPORATE);
        user.setCreatedAt(new Date());
        return user;
    }

    public static Store toStoreEntity(StoreRegisterDTO dto, User user) {
        Store store = new Store();
        store.setStoreName(dto.getStoreName());
        store.setCompanyName(dto.getCompanyName());
        store.setTaxNumber(dto.getTaxNumber());
        store.setTaxOffice(dto.getTaxOffice());
        store.setComponyAddress(dto.getComponyAddress());
        store.setOwner(user);
        return store;
    }

}
