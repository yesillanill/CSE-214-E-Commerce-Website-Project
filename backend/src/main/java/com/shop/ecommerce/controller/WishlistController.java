package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.wishlist.WishlistItemRequest;
import com.shop.ecommerce.dto.wishlist.WishlistItemResponse;
import com.shop.ecommerce.services.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping("/{userId}")
    public List<WishlistItemResponse> getWishlistItems(@PathVariable Long userId) {
        return wishlistService.getWishlistItems(userId);
    }

    @PostMapping("/add")
    public WishlistItemResponse addToWishlist(@RequestBody WishlistItemRequest request) {
        return wishlistService.addToWishlist(request);
    }

    @DeleteMapping("/remove/{wishlistItemId}")
    public void removeFromWishlist(@PathVariable Long wishlistItemId) {
        wishlistService.removeFromWishlist(wishlistItemId);
    }

    @DeleteMapping("/clear/{userId}")
    public void clearWishlist(@PathVariable Long userId) {
        wishlistService.clearWishlist(userId);
    }
}
