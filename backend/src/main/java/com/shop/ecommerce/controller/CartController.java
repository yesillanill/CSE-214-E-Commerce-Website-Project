package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.cart.CartItemRequest;
import com.shop.ecommerce.dto.cart.CartItemResponse;
import com.shop.ecommerce.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    public List<CartItemResponse> getCartItems(@PathVariable Long userId) {
        return cartService.getCartItems(userId);
    }

    @PostMapping("/add")
    public CartItemResponse addToCart(@RequestBody CartItemRequest request) {
        return cartService.addToCart(request);
    }

    @PatchMapping("/update/{cartItemId}")
    public CartItemResponse updateQuantity(@PathVariable Long cartItemId, @RequestParam Integer quantity) {
        return cartService.updateQuantity(cartItemId, quantity);
    }

    @DeleteMapping("/remove/{cartItemId}")
    public void removeCartItem(@PathVariable Long cartItemId) {
        cartService.removeCartItem(cartItemId);
    }

    @DeleteMapping("/clear/{userId}")
    public void clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
    }
}
