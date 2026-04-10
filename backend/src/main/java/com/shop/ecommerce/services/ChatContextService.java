package com.shop.ecommerce.services;

import com.shop.ecommerce.entities.*;
import com.shop.ecommerce.enums.Role;
import com.shop.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatContextService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistRepository wishlistRepository;
    private final WishlistItemsRepository wishlistItemsRepository;
    private final OrderRepository orderRepository;
    private final IndividualCustomerRepository individualCustomerRepository;
    private final AuditLogRepository auditLogRepository;
    private final ReviewRepository reviewRepository;

    private static final String BASE_URL = "http://localhost:4200";

    /**
     * Build a complete context string for the AI prompt based on the user's question, role, and identity.
     */
    public String buildContext(String question, String role, Long userId) {
        StringBuilder ctx = new StringBuilder();
        String q = question.toLowerCase(Locale.forLanguageTag("tr"));

        // 1. Public context — always available to everyone
        ctx.append(buildSiteStats());
        ctx.append(buildProductContext(q));
        ctx.append(buildCategoryList());
        ctx.append(buildStoreList());
        ctx.append(buildBrandList());

        // 2. Role-specific context — only when userId is provided
        if (userId != null) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Profile data for all authenticated users
                ctx.append(buildProfileContext(user));

                if (user.getRole() == Role.INDIVIDUAL) {
                    ctx.append(buildIndividualContext(userId, q));
                } else if (user.getRole() == Role.CORPORATE) {
                    ctx.append(buildCorporateContext(user, q));
                } else if (user.getRole() == Role.ADMIN) {
                    ctx.append(buildAdminContext(q));
                }
            }
        } else {
            ctx.append("\n[NOT LOGGED IN] The user is not logged in. For personal data (cart, wishlist, orders, profile), tell them to log in first.\n");
        }

        return ctx.toString();
    }

    // ===== PUBLIC CONTEXT =====================================================

    private String buildSiteStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SITE STATISTICS ===\n");
        sb.append("Total products: ").append(productRepository.count()).append("\n");
        sb.append("Total stores: ").append(storeRepository.count()).append("\n");
        sb.append("Total categories: ").append(categoryRepository.count()).append("\n");
        sb.append("Total brands: ").append(brandRepository.count()).append("\n");
        sb.append("Total users: ").append(userRepository.count()).append("\n\n");
        return sb.toString();
    }

    private String buildProductContext(String question) {
        StringBuilder sb = new StringBuilder();

        // Extract potential search keywords from the question
        List<String> keywords = extractKeywords(question);
        Set<Product> matchedProducts = new LinkedHashSet<>();

        // Search by keywords in product names
        for (String keyword : keywords) {
            if (keyword.length() >= 2) {
                List<Product> results = productRepository.searchByKeyword(keyword);
                matchedProducts.addAll(results);
            }
        }

        // Also check by category name
        List<Category> allCategories = categoryRepository.findAll();
        for (Category cat : allCategories) {
            if (question.contains(cat.getName().toLowerCase(Locale.forLanguageTag("tr")))) {
                List<Product> catProducts = productRepository.findByCategoryName(cat.getName());
                matchedProducts.addAll(catProducts);
            }
        }

        // Check by store name
        List<Store> allStores = storeRepository.findAll();
        for (Store store : allStores) {
            if (question.contains(store.getStoreName().toLowerCase(Locale.forLanguageTag("tr")))) {
                List<Product> storeProducts = productRepository.findByStoreName(store.getStoreName());
                matchedProducts.addAll(storeProducts);
            }
        }

        // Check by brand name
        List<Brand> allBrands = brandRepository.findAll();
        for (Brand brand : allBrands) {
            if (question.contains(brand.getName().toLowerCase(Locale.forLanguageTag("tr")))) {
                List<Product> brandProducts = productRepository.findByBrandName(brand.getName());
                matchedProducts.addAll(brandProducts);
            }
        }

        // If a general product question but no specific matches, show top products
        if (matchedProducts.isEmpty() && containsProductIntent(question)) {
            List<Product> topProducts = productRepository.findAll(PageRequest.of(0, 15)).getContent();
            matchedProducts.addAll(topProducts);
        }

        if (!matchedProducts.isEmpty()) {
            sb.append("=== MATCHED PRODUCTS ===\n");
            int count = 0;
            for (Product p : matchedProducts) {
                if (count >= 20) break;
                sb.append(formatProduct(p));
                count++;
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String buildCategoryList() {
        StringBuilder sb = new StringBuilder();
        List<Category> categories = categoryRepository.findAll();
        sb.append("=== ALL CATEGORIES ===\n");
        for (Category c : categories) {
            sb.append("- ").append(c.getName())
              .append(" | Link: ").append(BASE_URL).append("/products/category/").append(c.getName()).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String buildStoreList() {
        StringBuilder sb = new StringBuilder();
        List<Store> stores = storeRepository.findAll();
        sb.append("=== ALL STORES ===\n");
        for (Store s : stores) {
            sb.append("- ").append(s.getStoreName())
              .append(" (").append(s.getCompanyName() != null ? s.getCompanyName() : "").append(")")
              .append(" | Link: ").append(BASE_URL).append("/products/store/").append(s.getStoreName()).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String buildBrandList() {
        StringBuilder sb = new StringBuilder();
        List<Brand> brands = brandRepository.findAll();
        sb.append("=== ALL BRANDS ===\n");
        for (Brand b : brands) {
            sb.append("- ").append(b.getName())
              .append(" | Link: ").append(BASE_URL).append("/products/brand/").append(b.getName()).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    // ===== PROFILE CONTEXT (all authenticated users) ==========================

    private String buildProfileContext(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== USER PROFILE ===\n");
        sb.append("Name: ").append(user.getName()).append(" ").append(user.getSurname()).append("\n");
        sb.append("Email: ").append(user.getEmail()).append("\n");
        sb.append("Phone: ").append(user.getPhone()).append("\n");
        sb.append("Role: ").append(user.getRole()).append("\n");
        sb.append("Registered: ").append(user.getCreatedAt()).append("\n");

        if (user.getRole() == Role.INDIVIDUAL) {
            Optional<IndividualCustomer> custOpt = individualCustomerRepository.findByUser(user);
            if (custOpt.isPresent()) {
                IndividualCustomer c = custOpt.get();
                sb.append("Gender: ").append(c.getGender()).append("\n");
                sb.append("Birth Date: ").append(c.getBirthDate()).append("\n");
                sb.append("City: ").append(c.getCity()).append("\n");
                sb.append("Country: ").append(c.getCountry()).append("\n");
                sb.append("Membership: ").append(c.getMembershipType()).append("\n");
                sb.append("Total Spend: ").append(c.getTotalSpend()).append("\n");
                sb.append("Items Purchased: ").append(c.getItemsPurchased()).append("\n");
                sb.append("Average Rating: ").append(c.getAvgRating()).append("\n");
                sb.append("Satisfaction: ").append(c.getSatisfactionLevel()).append("\n");
            }
        } else if (user.getRole() == Role.CORPORATE) {
            Optional<Store> storeOpt = storeRepository.findByOwner(user);
            if (storeOpt.isPresent()) {
                Store store = storeOpt.get();
                sb.append("Store: ").append(store.getStoreName()).append("\n");
                sb.append("Company: ").append(store.getCompanyName()).append("\n");
                sb.append("Tax Number: ").append(store.getTaxNumber()).append("\n");
                sb.append("Tax Office: ").append(store.getTaxOffice()).append("\n");
                sb.append("Company Address: ").append(store.getComponyAddress()).append("\n");
                sb.append("Total Revenue: ").append(store.getTotalRevenue()).append("\n");
            }
        }

        sb.append("\n");
        return sb.toString();
    }

    // ===== INDIVIDUAL USER CONTEXT ============================================

    private String buildIndividualContext(Long userId, String question) {
        StringBuilder sb = new StringBuilder();

        // Cart
        if (containsCartIntent(question)) {
            sb.append(buildCartContext(userId));
        }

        // Wishlist
        if (containsWishlistIntent(question)) {
            sb.append(buildWishlistContext(userId));
        }

        // Orders
        if (containsOrderIntent(question)) {
            sb.append(buildOrderContext(userId));
        }

        // Statistics
        if (containsStatsIntent(question)) {
            sb.append(buildUserStatsContext(userId));
        }

        return sb.toString();
    }

    private String buildCartContext(Long userId) {
        StringBuilder sb = new StringBuilder();
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        sb.append("=== YOUR CART ===\n");
        if (cartOpt.isPresent()) {
            List<CartItem> items = cartItemRepository.findByCartId(cartOpt.get().getCartId());
            if (items.isEmpty()) {
                sb.append("Your cart is empty.\n");
            } else {
                for (CartItem item : items) {
                    Product p = item.getProduct();
                    sb.append("- ").append(p.getName())
                      .append(" x").append(item.getQuantity())
                      .append(" | Price: ").append(p.getPrice())
                      .append(" | Link: ").append(BASE_URL).append("/products/").append(p.getId()).append("\n");
                }
            }
        } else {
            sb.append("Your cart is empty.\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String buildWishlistContext(Long userId) {
        StringBuilder sb = new StringBuilder();
        Optional<Wishlist> wishlistOpt = wishlistRepository.findByUserId(userId);
        sb.append("=== YOUR WISHLIST ===\n");
        if (wishlistOpt.isPresent()) {
            List<WishlistItems> items = wishlistItemsRepository.findByWishlistId(wishlistOpt.get().getId());
            if (items.isEmpty()) {
                sb.append("Your wishlist is empty.\n");
            } else {
                for (WishlistItems item : items) {
                    Product p = item.getProduct();
                    sb.append("- ").append(p.getName())
                      .append(" | Price: ").append(p.getPrice())
                      .append(" | Link: ").append(BASE_URL).append("/products/").append(p.getId()).append("\n");
                }
            }
        } else {
            sb.append("Your wishlist is empty.\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String buildOrderContext(Long userId) {
        StringBuilder sb = new StringBuilder();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return "";

        List<Order> orders = orderRepository.findByUser(user);
        sb.append("=== YOUR ORDERS ===\n");
        if (orders.isEmpty()) {
            sb.append("You have no orders.\n");
        } else {
            int shown = 0;
            for (Order o : orders) {
                if (shown >= 10) break;
                sb.append("- Order #").append(o.getId())
                  .append(" | Date: ").append(o.getCreatedAt())
                  .append(" | Total: ").append(o.getGrandTotal())
                  .append(" | Items: ").append(o.getOrderItems() != null ? o.getOrderItems().size() : 0)
                  .append("\n");
                shown++;
            }
        }
        sb.append("Total orders: ").append(orderRepository.countByUserId(userId)).append("\n");
        sb.append("Total spent: ").append(orderRepository.sumGrandTotalByUserId(userId)).append("\n\n");
        return sb.toString();
    }

    private String buildUserStatsContext(Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== YOUR STATISTICS ===\n");
        sb.append("Total orders: ").append(orderRepository.countByUserId(userId)).append("\n");
        sb.append("Total spent: ").append(orderRepository.sumGrandTotalByUserId(userId)).append("\n\n");
        return sb.toString();
    }

    // ===== CORPORATE USER CONTEXT =============================================

    private String buildCorporateContext(User user, String question) {
        StringBuilder sb = new StringBuilder();
        Optional<Store> storeOpt = storeRepository.findByOwner(user);
        if (storeOpt.isEmpty()) return "";
        Store store = storeOpt.get();

        // Store orders
        if (containsOrderIntent(question)) {
            Page<Order> storeOrders = orderRepository.findByStoreId(store.getId(), PageRequest.of(0, 10));
            sb.append("=== YOUR STORE ORDERS ===\n");
            for (Order o : storeOrders.getContent()) {
                sb.append("- Order #").append(o.getId())
                  .append(" | Date: ").append(o.getCreatedAt())
                  .append(" | Total: ").append(o.getGrandTotal())
                  .append(" | Customer: ").append(o.getUser().getName()).append(" ").append(o.getUser().getSurname())
                  .append("\n");
            }
            sb.append("Total store orders: ").append(storeOrders.getTotalElements()).append("\n\n");
        }

        // Inventory / products
        if (containsInventoryIntent(question) || containsProductIntent(question)) {
            sb.append(buildStoreInventoryContext(store));
        }

        // Store stats
        if (containsStatsIntent(question)) {
            sb.append("=== YOUR STORE STATISTICS ===\n");
            sb.append("Store Name: ").append(store.getStoreName()).append("\n");
            sb.append("Total Revenue: ").append(store.getTotalRevenue()).append("\n");
            long productCount = productRepository.countByStoreId(store.getId());
            sb.append("Total Products: ").append(productCount).append("\n\n");
        }

        return sb.toString();
    }

    private String buildStoreInventoryContext(Store store) {
        StringBuilder sb = new StringBuilder();
        Page<Product> products = productRepository.findByStoreId(store.getId(), PageRequest.of(0, 20));
        sb.append("=== YOUR STORE INVENTORY ===\n");
        for (Product p : products.getContent()) {
            sb.append("- ").append(p.getName())
              .append(" | Price: ").append(p.getPrice())
              .append(" | Stock: ").append(p.getInventory() != null ? p.getInventory().getStock() : "N/A")
              .append(" | Sold: ").append(p.getSoldCount())
              .append(" | Rating: ").append(reviewRepository.averageRatingByProductId(p.getId()))
              .append(" | Link: ").append(BASE_URL).append("/products/").append(p.getId())
              .append("\n");
        }
        sb.append("Total products in store: ").append(products.getTotalElements()).append("\n\n");
        return sb.toString();
    }

    // ===== ADMIN CONTEXT ======================================================

    private String buildAdminContext(String question) {
        StringBuilder sb = new StringBuilder();

        // User aggregate info (without personal details)
        if (containsUserInfoIntent(question)) {
            long totalUsers = userRepository.count();
            sb.append("=== USER SUMMARY (aggregate only) ===\n");
            sb.append("Total users: ").append(totalUsers).append("\n");
            sb.append("Note: Individual user details (emails, phones, carts, wishlists) are private and cannot be shared through the AI assistant.\n\n");
        }

        // Audit logs (summary only)
        if (containsAuditIntent(question)) {
            Page<AuditLog> logs = auditLogRepository.findAll(PageRequest.of(0, 20));
            sb.append("=== RECENT AUDIT LOGS ===\n");
            for (AuditLog al : logs.getContent()) {
                sb.append("- ID: ").append(al.getId())
                  .append(" | Action: ").append(al.getAction())
                  .append(" | Entity: ").append(al.getTargetEntity())
                  .append(" | Date: ").append(al.getCreatedAt())
                  .append("\n");
            }
            sb.append("Total audit logs: ").append(logs.getTotalElements()).append("\n\n");
        }

        // Store summary for admin (no revenue details)
        if (containsStoreInfoIntent(question)) {
            List<Store> stores = storeRepository.findAll();
            sb.append("=== ALL STORES SUMMARY ===\n");
            for (Store s : stores) {
                long productCount = productRepository.countByStoreId(s.getId());
                sb.append("- ").append(s.getStoreName())
                  .append(" | Products: ").append(productCount)
                  .append(" | Link: ").append(BASE_URL).append("/products/store/").append(s.getStoreName())
                  .append("\n");
            }
            sb.append("Note: Individual store revenue and detailed financials are private and cannot be shared.\n\n");
        }

        // Platform-wide stats
        if (containsStatsIntent(question)) {
            sb.append("=== PLATFORM STATISTICS ===\n");
            sb.append("Total Users: ").append(userRepository.count()).append("\n");
            sb.append("Total Orders: ").append(orderRepository.count()).append("\n");
            sb.append("Total Products: ").append(productRepository.count()).append("\n");
            sb.append("Total Stores: ").append(storeRepository.count()).append("\n");
            sb.append("Total Categories: ").append(categoryRepository.count()).append("\n");
            sb.append("Total Brands: ").append(brandRepository.count()).append("\n\n");
        }

        return sb.toString();
    }

    // ===== INTENT DETECTION ===================================================

    private boolean containsProductIntent(String q) {
        return matchesAny(q, "ürün", "product", "ürünler", "products", "fiyat", "price",
                "en ucuz", "en pahalı", "cheapest", "expensive", "ne var", "satılan");
    }

    private boolean containsCartIntent(String q) {
        return matchesAny(q, "sepet", "cart", "sepetim", "sepetimde", "my cart");
    }

    private boolean containsWishlistIntent(String q) {
        return matchesAny(q, "istek", "wishlist", "favori", "beğen", "istek listem",
                "wish list", "favorilerim", "beğendiklerim");
    }

    private boolean containsOrderIntent(String q) {
        return matchesAny(q, "sipariş", "order", "siparişlerim", "orders", "satın",
                "aldığım", "purchases", "satış", "sales", "satışlar");
    }

    private boolean containsStatsIntent(String q) {
        return matchesAny(q, "istatistik", "stats", "statistics", "topla", "total",
                "kaç", "how many", "count", "özet", "summary", "genel", "analiz", "analytics");
    }

    private boolean containsInventoryIntent(String q) {
        return matchesAny(q, "envanter", "inventory", "stok", "stock", "depom",
                "stoklarım", "ürünlerim", "my products");
    }

    private boolean containsUserInfoIntent(String q) {
        return matchesAny(q, "kullanıcı", "user", "müşteri", "customer", "üye",
                "member", "kullanıcılar", "users", "müşteriler", "customers");
    }

    private boolean containsAuditIntent(String q) {
        return matchesAny(q, "audit", "denetim", "log", "günlük", "kayıt",
                "audit log", "denetim kaydı", "işlem geçmişi", "aktivite");
    }

    private boolean containsStoreInfoIntent(String q) {
        return matchesAny(q, "mağaza", "store", "dükkan", "shop", "mağazalar",
                "stores", "satıcı", "seller");
    }

    // ===== HELPERS =============================================================

    private boolean matchesAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private List<String> extractKeywords(String question) {
        // Remove common Turkish/English stop words and return meaningful keywords
        Set<String> stopWords = Set.of(
                "bir", "bu", "şu", "ve", "ile", "için", "olan", "var", "mı", "mi", "mu", "mü",
                "ne", "nedir", "neler", "nasıl", "kaç", "the", "a", "an", "is", "are", "what",
                "which", "how", "do", "does", "can", "could", "about", "hakkında", "göster",
                "show", "list", "listele", "bana", "benim", "bul", "find", "ara", "search",
                "hangi", "tell", "söyle", "anlat", "lütfen", "please"
        );

        String cleaned = question.replaceAll("[?.,!;:'\"]", " ");
        String[] words = cleaned.split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String word : words) {
            String w = word.trim().toLowerCase(Locale.forLanguageTag("tr"));
            if (w.length() >= 2 && !stopWords.contains(w)) {
                keywords.add(w);
            }
        }
        return keywords;
    }

    private String formatProduct(Product p) {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(p.getName());
        sb.append(" | Price: ").append(p.getPrice());
        sb.append(" | Link: ").append(BASE_URL).append("/products/").append(p.getId());
        sb.append("\n");
        return sb.toString();
    }
}
