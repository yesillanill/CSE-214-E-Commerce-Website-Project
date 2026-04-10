package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.cart.CartItemRequest;
import com.shop.ecommerce.dto.cart.CartItemResponse;
import com.shop.ecommerce.entities.Cart;
import com.shop.ecommerce.entities.CartItem;
import com.shop.ecommerce.entities.Product;
import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.enums.Role;
import com.shop.ecommerce.mapper.ProductMapper;
import com.shop.ecommerce.repository.CartItemRepository;
import com.shop.ecommerce.repository.CartRepository;
import com.shop.ecommerce.repository.IndividualCustomerRepository;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.ReviewRepository;
import com.shop.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final IndividualCustomerRepository individualCustomerRepository;

    public List<CartItemResponse> getCartItems(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<CartItem> items = cartItemRepository.findByCartId(cartOpt.get().getCartId());
        return items.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public CartItemResponse addToCart(CartItemRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.ADMIN || user.getRole() == Role.CORPORATE) {
            throw new RuntimeException("Admins and Corporates cannot add items to cart.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Cart cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setIndividualCustomer(individualCustomerRepository.findByUser(user).orElseThrow());
            return cartRepository.save(newCart);
        });

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getCartId(), product.getId())
                .orElse(new CartItem());

        if (cartItem.getId() == null) {
            cartItem.setCartId(cart.getCartId());
            cartItem.setProductId(product.getId());
            cartItem.setQuantity(request.getQuantity());
            cartItem.setCart(cart);
            cartItem.setProduct(product);
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        }

        cartItem = cartItemRepository.save(cartItem);
        return mapToResponse(cartItem);
    }

    @Transactional
    public CartItemResponse updateQuantity(Long cartItemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        cartItem.setQuantity(quantity);
        cartItem = cartItemRepository.save(cartItem);
        return mapToResponse(cartItem);
    }

    @Transactional
    public void removeCartItem(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    @Transactional
    public void clearCart(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        cartOpt.ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getCartId()));
    }

    private CartItemResponse mapToResponse(CartItem cartItem) {
        CartItemResponse response = new CartItemResponse();
        response.setId(cartItem.getId());
        response.setQuantity(cartItem.getQuantity());
        long productId = cartItem.getProduct().getId();
        double avgRating = reviewRepository.averageRatingByProductId(productId);
        long reviewCount = reviewRepository.countByProductId(productId);
        response.setProduct(ProductMapper.toListDTO(cartItem.getProduct(), avgRating, reviewCount));
        return response;
    }
}
