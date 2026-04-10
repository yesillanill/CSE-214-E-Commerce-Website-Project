package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.wishlist.WishlistItemRequest;
import com.shop.ecommerce.dto.wishlist.WishlistItemResponse;
import com.shop.ecommerce.entities.Product;
import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.entities.Wishlist;
import com.shop.ecommerce.entities.WishlistItems;
import com.shop.ecommerce.enums.Role;
import com.shop.ecommerce.mapper.ProductMapper;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.ReviewRepository;
import com.shop.ecommerce.repository.UserRepository;
import com.shop.ecommerce.repository.IndividualCustomerRepository;
import com.shop.ecommerce.repository.WishlistItemsRepository;
import com.shop.ecommerce.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistItemsRepository wishlistItemsRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final IndividualCustomerRepository individualCustomerRepository;

    public List<WishlistItemResponse> getWishlistItems(Long userId) {
        Optional<Wishlist> wishlistOpt = wishlistRepository.findByUserId(userId);
        if (wishlistOpt.isEmpty()) {
            return Collections.emptyList();
        }

        List<WishlistItems> items = wishlistItemsRepository.findByWishlistId(wishlistOpt.get().getId());
        return items.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public WishlistItemResponse addToWishlist(WishlistItemRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.ADMIN || user.getRole() == Role.CORPORATE) {
            throw new RuntimeException("Admins and Corporates cannot add items to wishlist.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Wishlist wishlist = wishlistRepository.findByUserId(user.getId()).orElseGet(() -> {
            Wishlist newWishlist = new Wishlist();
            newWishlist.setIndividualCustomer(individualCustomerRepository.findByUser(user).orElseThrow());
            return wishlistRepository.save(newWishlist);
        });

        Optional<WishlistItems> existingItem = wishlistItemsRepository.findByWishlistIdAndProductId(wishlist.getId(),
                product.getId());
        if (existingItem.isPresent()) {
            return mapToResponse(existingItem.get());
        }

        WishlistItems wishlistItem = new WishlistItems();
        wishlistItem.setWishlistId(wishlist.getId());
        wishlistItem.setProductId(product.getId());
        wishlistItem.setWishlist(wishlist);
        wishlistItem.setProduct(product);

        wishlistItem = wishlistItemsRepository.save(wishlistItem);
        return mapToResponse(wishlistItem);
    }

    @Transactional
    public void removeFromWishlist(Long wishlistItemId) {
        wishlistItemsRepository.deleteById(wishlistItemId);
    }

    @Transactional
    public void clearWishlist(Long userId) {
        Optional<Wishlist> wishlistOpt = wishlistRepository.findByUserId(userId);
        wishlistOpt.ifPresent(wishlist -> wishlistItemsRepository.deleteByWishlistId(wishlist.getId()));
    }

    private WishlistItemResponse mapToResponse(WishlistItems wishlistItem) {
        WishlistItemResponse response = new WishlistItemResponse();
        response.setId(wishlistItem.getId());
        long productId = wishlistItem.getProduct().getId();
        double avgRating = reviewRepository.averageRatingByProductId(productId);
        long reviewCount = reviewRepository.countByProductId(productId);
        response.setProduct(ProductMapper.toListDTO(wishlistItem.getProduct(), avgRating, reviewCount));
        return response;
    }
}
