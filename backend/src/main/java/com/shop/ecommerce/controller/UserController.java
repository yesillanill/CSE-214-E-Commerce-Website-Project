package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.user.UserProfileDTO;
import com.shop.ecommerce.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<UserProfileDTO> updateUserProfile(@PathVariable Long userId, @RequestBody UserProfileDTO dto) {
        return ResponseEntity.ok(userService.updateUserProfile(userId, dto));
    }
}
