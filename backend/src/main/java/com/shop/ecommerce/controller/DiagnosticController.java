package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.order.OrderDTO;
import com.shop.ecommerce.services.OrderService;
import com.shop.ecommerce.repository.*;
import com.shop.ecommerce.entities.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/diagnostic")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class DiagnosticController {
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreRepository storeRepository;

    @GetMapping("/check")
    public Map<String, Object> check(@RequestParam(required = false) Long userId, @RequestParam(required = false) Long storeId) {
        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("currentTime", new Date());
        debug.put("counts", Map.of(
            "users", userRepository.count(),
            "orders", orderRepository.count(),
            "items", orderItemRepository.count(),
            "stores", storeRepository.count()
        ));

        if (userId != null) {
            Optional<User> u = userRepository.findById(userId);
            debug.put("userId", userId);
            debug.put("userExists", u.isPresent());
            if (u.isPresent()) {
                debug.put("userEmail", u.get().getEmail());
                debug.put("userRole", u.get().getRole());
            }
            long count = orderRepository.countByUserId(userId);
            debug.put("ordersForUser", count);
        }

        if (storeId != null) {
            Optional<Store> s = storeRepository.findById(storeId);
            debug.put("storeId", storeId);
            debug.put("storeExists", s.isPresent());
            if (s.isPresent()) {
                debug.put("storeName", s.get().getStoreName());
            }
            // Manual check for store orders
            long storeOrdersCount = orderRepository.findAll().stream()
                .filter(o -> o.getOrderItems().stream().anyMatch(oi -> oi.getProduct().getStore().getId().equals(storeId)))
                .count();
            debug.put("ordersForStore", storeOrdersCount);
        }

        return debug;
    }

    @GetMapping("/raw-orders")
    public List<Map<String, Object>> getRawOrders() {
        List<Order> orders = orderRepository.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Order o : orders.subList(0, Math.min(10, orders.size()))) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("userId", o.getUser() != null ? o.getUser().getId() : null);
            m.put("grandTotal", o.getGrandTotal());
            list.add(m);
        }
        return list;
    }
}
