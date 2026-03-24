package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.order.OrderDTO;
import com.shop.ecommerce.entities.Payment;
import com.shop.ecommerce.entities.Shipment;
import com.shop.ecommerce.enums.PaymentStatus;
import com.shop.ecommerce.enums.ShipmentStatus;
import com.shop.ecommerce.repository.PaymentRepository;
import com.shop.ecommerce.repository.ShipmentRepository;
import com.shop.ecommerce.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/store-orders")
@RequiredArgsConstructor
@CrossOrigin
public class StoreOrderController {

    private final OrderService orderService;
    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping
    public ResponseEntity<Page<OrderDTO>> getStoreOrders(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(orderService.getOrdersByStore(storeId, pageable, startDate, endDate));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body
    ) {
        String newStatus = body.get("status");
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Shipment not found for order " + orderId));

        shipment.setStatus(ShipmentStatus.valueOf(newStatus));
        if ("SHIPPED".equals(newStatus)) {
            shipment.setShipmentDate(java.time.LocalDate.now());
        }
        shipmentRepository.save(shipment);

        if ("REJECTED".equals(newStatus)) {
            paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            });
        }

        return ResponseEntity.ok(Map.of("status", "updated"));
    }
}
