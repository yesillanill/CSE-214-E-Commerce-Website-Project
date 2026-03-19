package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.user.UserProfileDTO;
import com.shop.ecommerce.entities.IndividualCustomer;
import com.shop.ecommerce.entities.Store;
import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.enums.Role;
import com.shop.ecommerce.repository.IndividualCustomerRepository;
import com.shop.ecommerce.repository.StoreRepository;
import com.shop.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final IndividualCustomerRepository individualCustomerRepository;
    private final StoreRepository storeRepository;

    public UserProfileDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setSurname(user.getSurname());
        dto.setEmail(user.getEmail());
        dto.setPassword(user.getPassword());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());

        if (user.getRole() == Role.INDIVIDUAL) {
            Optional<IndividualCustomer> customerOpt = individualCustomerRepository.findByUser(user);
            if (customerOpt.isPresent()) {
                IndividualCustomer customer = customerOpt.get();
                dto.setGender(customer.getGender());
                dto.setBirthdate(customer.getBirthDate());
                dto.setStreet(customer.getStreet());
                dto.setCity(customer.getCity());
                dto.setPostalCode(customer.getPostalCode());
                dto.setCountry(customer.getCountry());
                dto.setMembershipType(customer.getMembershipType());
            }
        } else if (user.getRole() == Role.CORPORATE) {
            Optional<Store> storeOpt = storeRepository.findByOwner(user);
            if (storeOpt.isPresent()) {
                Store store = storeOpt.get();
                dto.setStoreName(store.getStoreName());
                dto.setCompanyName(store.getCompanyName());
                dto.setTaxNumber(store.getTaxNumber());
                dto.setTaxOffice(store.getTaxOffice());
                dto.setCompanyAddress(store.getComponyAddress());
            }
        }

        return dto;
    }

    @Transactional
    public UserProfileDTO updateUserProfile(Long userId, UserProfileDTO updateDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updateDto.getName() != null) user.setName(updateDto.getName());
        if (updateDto.getSurname() != null) user.setSurname(updateDto.getSurname());
        if (updateDto.getEmail() != null) user.setEmail(updateDto.getEmail());
        if (updateDto.getPassword() != null && !updateDto.getPassword().isEmpty()) user.setPassword(updateDto.getPassword());
        if (updateDto.getPhone() != null) user.setPhone(updateDto.getPhone());

        user = userRepository.save(user);

        if (user.getRole() == Role.INDIVIDUAL) {
            IndividualCustomer customer = individualCustomerRepository.findByUser(user)
                    .orElse(new IndividualCustomer());
            customer.setUser(user);
            if (updateDto.getGender() != null) customer.setGender(updateDto.getGender());
            if (updateDto.getBirthdate() != null) customer.setBirthDate(updateDto.getBirthdate());
            if (updateDto.getStreet() != null) customer.setStreet(updateDto.getStreet());
            if (updateDto.getCity() != null) customer.setCity(updateDto.getCity());
            if (updateDto.getPostalCode() != null) customer.setPostalCode(updateDto.getPostalCode());
            if (updateDto.getCountry() != null) customer.setCountry(updateDto.getCountry());
            individualCustomerRepository.save(customer);
        } else if (user.getRole() == Role.CORPORATE) {
            Store store = storeRepository.findByOwner(user)
                    .orElse(new Store());
            store.setOwner(user);
            if (updateDto.getStoreName() != null) store.setStoreName(updateDto.getStoreName());
            if (updateDto.getCompanyName() != null) store.setCompanyName(updateDto.getCompanyName());
            if (updateDto.getTaxNumber() != null) store.setTaxNumber(updateDto.getTaxNumber());
            if (updateDto.getTaxOffice() != null) store.setTaxOffice(updateDto.getTaxOffice());
            if (updateDto.getCompanyAddress() != null) store.setComponyAddress(updateDto.getCompanyAddress());
            storeRepository.save(store);
        }

        return getUserProfile(userId);
    }
}
