package com.shop.ecommerce.controller;

import com.shop.ecommerce.entities.Store;
import com.shop.ecommerce.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
public class AdminStoreController {

    private final StoreRepository storeRepository;

    @GetMapping
    public ResponseEntity<Page<Store>> getStores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "storeName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        if (search != null && !search.isEmpty()) {
            return ResponseEntity.ok(storeRepository.searchStores(search, pageable));
        }
        return ResponseEntity.ok(storeRepository.findAll(pageable));
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<Store> getStore(@PathVariable Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        return ResponseEntity.ok(store);
    }
}
