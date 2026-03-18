package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.auth.*;
import com.shop.ecommerce.entities.IndividualCustomer;
import com.shop.ecommerce.entities.Store;
import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.mapper.AuthMapper;
import com.shop.ecommerce.repository.IndividualCustomerRepository;
import com.shop.ecommerce.repository.StoreRepository;
import com.shop.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final IndividualCustomerRepository individualCustomerRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public User registerIndividual(IndividualRegisterDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent() || userRepository.findByPhone(dto.getPhone()).isPresent()) {
            throw new RuntimeException("User with this email or phone already exists");
        }
        User user = AuthMapper.toUserEntityFromIndividual(dto);
        user = userRepository.save(user);

        IndividualCustomer customer = AuthMapper.toIndividualCustomerEntity(dto, user);
        individualCustomerRepository.save(customer);

        return user;
    }

    @Transactional
    public User registerStore(StoreRegisterDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent() || userRepository.findByPhone(dto.getPhone()).isPresent()) {
            throw new RuntimeException("User with this email or phone already exists");
        }
        User user = AuthMapper.toUserEntityFromStore(dto);
        user = userRepository.save(user);

        Store store = AuthMapper.toStoreEntity(dto, user);
        storeRepository.save(store);

        return user;
    }

    public User loginWithEmail(EmailLoginDTO dto) {
        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(dto.getPassword())) {
            return userOpt.get();
        }
        throw new RuntimeException("Invalid email or password");
    }

    public User loginWithPhone(PhoneLoginDTO dto) {
        Optional<User> userOpt = userRepository.findByPhone(dto.getPhone());
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(dto.getPassword())) {
            return userOpt.get();
        }
        throw new RuntimeException("Invalid phone or password");
    }
}
