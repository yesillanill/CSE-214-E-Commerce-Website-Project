package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.order.CheckoutRequest;
import com.shop.ecommerce.dto.order.OrderDTO;
import com.shop.ecommerce.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<OrderDTO>> getOrders(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(orderService.getOrdersByUser(userId, pageable, startDate, endDate));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request) {
        try {
            return ResponseEntity.ok(orderService.checkout(request));
        } catch (Exception e) {
            System.err.println("Checkout failed: " + e.getMessage());
            String message = "Siparişiniz işlenirken bir hata oluştu / An error occurred while processing your order.";
            if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                message = "Sistem hatası (Veritabanı uyuşmazlığı) / System error (Database conflict). Lütfen yetkiliyle iletişime geçin / Please contact administrator.";
            } else if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                message += " Detay/Detail: " + e.getMessage();
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("message", message));
        }
    }
}
