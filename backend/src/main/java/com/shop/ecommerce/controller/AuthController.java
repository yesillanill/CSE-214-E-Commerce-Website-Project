package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.auth.*;
import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/individual")
    public ResponseEntity<User> registerIndividual(@RequestBody IndividualRegisterDTO dto) {
        return ResponseEntity.ok(authService.registerIndividual(dto));
    }

    @PostMapping("/register/store")
    public ResponseEntity<User> registerStore(@RequestBody StoreRegisterDTO dto) {
        return ResponseEntity.ok(authService.registerStore(dto));
    }

    @PostMapping("/login/email")
    public ResponseEntity<User> loginEmail(@RequestBody EmailLoginDTO dto) {
        return ResponseEntity.ok(authService.loginWithEmail(dto));
    }

    @PostMapping("/login/phone")
    public ResponseEntity<User> loginPhone(@RequestBody PhoneLoginDTO dto) {
        return ResponseEntity.ok(authService.loginWithPhone(dto));
    }

    @PostMapping("/logout/{userId}")
    public ResponseEntity<Void> logout(@PathVariable Long userId) {
        authService.logout(userId);
        return ResponseEntity.ok().build();
    }
}
