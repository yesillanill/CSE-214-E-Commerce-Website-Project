package com.shop.ecommerce.controller;

import com.shop.ecommerce.entities.Store;
import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.repository.StoreRepository;
import com.shop.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class StoreController {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @GetMapping("/my-store")
    public ResponseEntity<?> getMyStore(@RequestParam Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Optional<Store> store = storeRepository.findByOwner(user);
        if (store.isPresent()) {
            return ResponseEntity.ok(store.get());
        }
        return ResponseEntity.status(404).body(Map.of("error", "No store found"));
    }
}
