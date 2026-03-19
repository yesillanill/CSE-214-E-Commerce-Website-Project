package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.order.OrderDTO;
import com.shop.ecommerce.dto.order.OrderItemDTO;
import com.shop.ecommerce.entities.*;
import com.shop.ecommerce.repository.OrderRepository;
import com.shop.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public Page<OrderDTO> getOrdersByUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUser(user, pageable).map(this::toDTO);
    }

    public Page<OrderDTO> getOrdersByStore(Long storeId, Pageable pageable) {
        return orderRepository.findByStoreId(storeId, pageable).map(this::toDTO);
    }

    private OrderDTO toDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setGrandTotal(order.getGrandTotal());
        dto.setShippingAddress(order.getShippingAddress());

        if (order.getPayment() != null) {
            Payment p = order.getPayment();
            dto.setPaymentStatus(p.getPaymentStatus() != null ? p.getPaymentStatus().name() : null);
            dto.setPaymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null);
        }

        if (order.getShipment() != null) {
            Shipment s = order.getShipment();
            dto.setShippingMethod(s.getShippingMethod() != null ? s.getShippingMethod().name() : null);
            dto.setShippingCost(s.getShippingCost());
            dto.setShipmentDate(s.getShipmentDate());
            dto.setShipmentStatus(s.getStatus() != null ? s.getStatus().name() : null);
            dto.setDeliveryDate(s.getDeliveryDate());
            dto.setEstimatedDelivery(s.getEstimatedDelivery());
            dto.setTrackingNumber(s.getTrackingNumber());
        }

        if (order.getOrderItems() != null) {
            dto.setOrderItems(order.getOrderItems().stream().map(this::toItemDTO).toList());
        }

        return dto;
    }

    private OrderItemDTO toItemDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setUnitPrice(item.getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setLineTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

        if (item.getProduct() != null) {
            Product product = item.getProduct();
            dto.setProductName(product.getName());
            dto.setProductId(product.getId());

            if (product.getBrand() != null) {
                dto.setBrandName(product.getBrand().getName());
                dto.setBrandId(product.getBrand().getId());
            }
            if (product.getStore() != null) {
                dto.setStoreName(product.getStore().getStoreName());
                dto.setStoreId(product.getStore().getId());
            }
        }
        return dto;
    }
}
