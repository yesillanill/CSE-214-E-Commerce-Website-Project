package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.order.CheckoutRequest;
import com.shop.ecommerce.dto.order.OrderDTO;
import com.shop.ecommerce.dto.order.OrderItemDTO;
import com.shop.ecommerce.entities.*;
import com.shop.ecommerce.enums.PaymentMethod;
import com.shop.ecommerce.enums.PaymentStatus;
import com.shop.ecommerce.enums.ShipmentStatus;
import com.shop.ecommerce.enums.ShippingMethod;
import com.shop.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public Page<OrderDTO> getOrdersByUser(Long userId, Pageable pageable, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (startDate != null || endDate != null) {
            java.time.LocalDateTime start = startDate != null ? startDate.atStartOfDay() : java.time.LocalDateTime.of(2000, 1, 1, 0, 0);
            java.time.LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : java.time.LocalDateTime.of(2100, 1, 1, 0, 0);
            return orderRepository.findByUserAndDateRange(user, start, end, pageable).map(this::toDTO);
        }
        return orderRepository.findByUser(user, pageable).map(this::toDTO);
    }

    public Page<OrderDTO> getOrdersByStore(Long storeId, Pageable pageable, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null || endDate != null) {
            java.time.LocalDateTime start = startDate != null ? startDate.atStartOfDay() : java.time.LocalDateTime.of(2000, 1, 1, 0, 0);
            java.time.LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : java.time.LocalDateTime.of(2100, 1, 1, 0, 0);
            return orderRepository.findByStoreIdAndDateRange(storeId, start, end, pageable).map(this::toDTO);
        }
        return orderRepository.findByStoreId(storeId, pageable).map(this::toDTO);
    }

    @Transactional
    public OrderDTO checkout(CheckoutRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getCartId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Create the order
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order = orderRepository.save(order);

        // Create order items from cart items
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            BigDecimal price = product.getPrice();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setPrice(price);
            orderItem.setQuantity(cartItem.getQuantity());
            order.getOrderItems().add(orderItem);

            grandTotal = grandTotal.add(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));

            // Update product sold count
            product.setSoldCount(product.getSoldCount() + cartItem.getQuantity());
            try {
                if (product.getInventory() != null) {
                    int newStock = product.getInventory().getStock() - cartItem.getQuantity();
                    product.getInventory().setStock(Math.max(0, newStock));
                }
            } catch (Exception ignored) {
                // Inventory update is best-effort
            }
            productRepository.save(product);
        }

        order.setGrandTotal(grandTotal);

        // Create payment record
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(grandTotal);

        // Use payment method from request
        if ("CASH_ON_DELIVERY".equals(request.getPaymentMethod())) {
            payment.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        } else {
            payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        }
        payment.setPaymentStatus(PaymentStatus.PENDING);
        order.setPayment(payment);

        // Create shipment record
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setShippingMethod(ShippingMethod.STANDARD);
        shipment.setShippingCost(BigDecimal.ZERO);
        shipment.setStatus(ShipmentStatus.PROCESSING);
        shipment.setEstimatedDelivery(LocalDate.now().plusDays(7));
        order.setShipment(shipment);

        order = orderRepository.save(order);

        return toDTO(order);
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
        BigDecimal unitPrice = item.getPrice();
        if (unitPrice == null && item.getProduct() != null && item.getProduct().getPrice() != null) {
            unitPrice = item.getProduct().getPrice();
        }
        dto.setUnitPrice(unitPrice);
        dto.setQuantity(item.getQuantity());
        dto.setLineTotal(unitPrice != null && item.getQuantity() != null
                ? unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
                : BigDecimal.ZERO);

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
