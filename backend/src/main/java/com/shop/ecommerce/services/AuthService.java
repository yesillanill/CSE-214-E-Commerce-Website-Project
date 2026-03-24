package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.auth.*;
import com.shop.ecommerce.entities.IndividualCustomer;
import com.shop.ecommerce.entities.Store;
import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.entities.AuditLog;
import com.shop.ecommerce.enums.AuditAction;
import com.shop.ecommerce.mapper.AuthMapper;
import com.shop.ecommerce.repository.IndividualCustomerRepository;
import com.shop.ecommerce.repository.StoreRepository;
import com.shop.ecommerce.repository.UserRepository;
import com.shop.ecommerce.repository.AuditLogRepository;
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
    private final AuditLogRepository auditLogRepository;

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
        String email = dto.getEmail() != null ? dto.getEmail().trim() : "";
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(dto.getPassword())) {
            User user = userOpt.get();
            logAction(user, AuditAction.USER_LOGIN, "User logged in with email");
            return user;
        }
        throw new RuntimeException("Invalid email or password");
    }

    public User loginWithPhone(PhoneLoginDTO dto) {
        String phone = dto.getPhone() != null ? dto.getPhone().trim() : "";
        Optional<User> userOpt = userRepository.findByPhone(phone);
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(dto.getPassword())) {
            User user = userOpt.get();
            logAction(user, AuditAction.USER_LOGIN, "User logged in with phone");
            return user;
        }
        throw new RuntimeException("Invalid phone or password");
    }

    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            logAction(user, AuditAction.USER_LOGOUT, "User logged out");
        });
    }

    private void logAction(User user, AuditAction action, String details) {
        AuditLog log = new AuditLog();
        log.setUserId(user.getId());
        log.setUserEmail(user.getEmail());
        log.setUserRole(user.getRole() != null ? user.getRole().name() : "UNKNOWN");
        log.setAction(action);
        log.setTargetEntity("User");
        log.setDetails(details);
        auditLogRepository.save(log);
    }
}
