package com.shop.ecommerce.controller;

import com.shop.ecommerce.entities.IndividualCustomer;
import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.enums.Role;
import com.shop.ecommerce.repository.IndividualCustomerRepository;
import com.shop.ecommerce.repository.OrderRepository;
import com.shop.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AdminCustomerController {

    private final UserRepository userRepository;
    private final IndividualCustomerRepository individualCustomerRepository;
    private final OrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCustomers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        List<User> allIndividuals = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.INDIVIDUAL)
                .filter(u -> search.isEmpty() ||
                        (u.getName() != null && u.getName().toLowerCase().contains(search.toLowerCase())) ||
                        (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.toLowerCase())))
                .toList();

        int start = page * size;
        int end = Math.min(start + size, allIndividuals.size());
        List<Map<String, Object>> customers = new ArrayList<>();

        for (User u : allIndividuals.subList(start, end)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("surname", u.getSurname());
            map.put("email", u.getEmail());
            map.put("createdAt", u.getCreatedAt());
            map.put("totalOrders", orderRepository.countByUserId(u.getId()));
            map.put("totalSpend", orderRepository.sumGrandTotalByUserId(u.getId()));

            individualCustomerRepository.findById(u.getId()).ifPresent(ic -> {
                map.put("membershipType", ic.getMembershipType() != null ? ic.getMembershipType().name() : null);
            });

            customers.add(map);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", customers);
        result.put("totalElements", allIndividuals.size());
        result.put("totalPages", (int) Math.ceil((double) allIndividuals.size() / size));
        result.put("number", page);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        userRepository.deleteById(userId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
