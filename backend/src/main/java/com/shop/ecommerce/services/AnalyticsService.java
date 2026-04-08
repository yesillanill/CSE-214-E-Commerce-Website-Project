package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.analytics.*;
import com.shop.ecommerce.entities.*;
import com.shop.ecommerce.enums.MembershipType;
import com.shop.ecommerce.enums.Role;
import com.shop.ecommerce.enums.SatisfactionLevel;
import com.shop.ecommerce.enums.ShipmentStatus;
import com.shop.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final IndividualCustomerRepository individualCustomerRepository;
    private final InventoryRepository inventoryRepository;
    private final ShipmentRepository shipmentRepository;
    private final AuditLogRepository auditLogRepository;

    // ─── PUBLIC STATS ─────────────────────────────────────

    public PublicStatsDTO getPublicStats() {
        return new PublicStatsDTO(
                userRepository.count(),
                productRepository.count(),
                categoryRepository.count(),
                productRepository.countDistinctBrands(),
                storeRepository.count()
        );
    }

    // ─── INDIVIDUAL ANALYTICS ─────────────────────────────

    public IndividualAnalyticsDTO getIndividualAnalytics(Long userId) {
        IndividualAnalyticsDTO dto = new IndividualAnalyticsDTO();

        // Cards
        BigDecimal totalSpend = orderRepository.sumGrandTotalByUserId(userId);
        dto.setTotalSpend(totalSpend.doubleValue());

        Long totalOrders = orderRepository.countByUserId(userId);
        dto.setTotalOrders(totalOrders);

        dto.setAvgOrderValue(totalOrders > 0 ? totalSpend.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP).doubleValue() : 0);

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            IndividualCustomer ic = individualCustomerRepository.findByUser(user).orElse(null);
            dto.setReviewCount(ic != null && ic.getItemsPurchased() != null ? ic.getItemsPurchased().longValue() : 0);
        }

        // Monthly spend (last 6 months)
        List<Order> userOrders = orderRepository.findByUserId(userId);
        dto.setMonthlySpend(buildMonthlySpend(userOrders, 6));

        // Order status distribution
        dto.setOrderStatusDist(buildOrderStatusDist(userOrders));

        // Category spend
        dto.setCategorySpend(buildCategorySpend(userOrders));

        // Recent 5 orders
        dto.setRecentOrders(buildRecentOrders(userOrders, 5));

        return dto;
    }

    // ─── CORPORATE ANALYTICS ──────────────────────────────

    public CorporateAnalyticsDTO getCorporateAnalytics(Long userId) {
        CorporateAnalyticsDTO dto = new CorporateAnalyticsDTO();

        User user = userRepository.findById(userId).orElseThrow();
        Store store = storeRepository.findByOwner(user).orElseThrow();
        Long storeId = store.getId();

        List<Order> storeOrders = orderRepository.findAllByStoreId(storeId);

        // Cards
        double totalRevenue = storeOrders.stream()
                .map(Order::getGrandTotal)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
        dto.setTotalRevenue(totalRevenue);
        dto.setTotalOrders(storeOrders.size());

        // Active customers (distinct users who ordered)
        long activeCustomers = storeOrders.stream()
                .map(o -> o.getUser().getId())
                .distinct()
                .count();
        dto.setActiveCustomers(activeCustomers);

        // Average rating from products of this store
        List<Product> storeProducts = productRepository.findByStoreId(storeId);
        double avgRating = storeProducts.stream()
                .filter(p -> p.getRating() > 0)
                .mapToDouble(Product::getRating)
                .average()
                .orElse(0.0);
        dto.setAvgRating(Math.round(avgRating * 100.0) / 100.0);

        // Revenue series (last 6 months)
        dto.setRevenueSeries(buildMonthlySpend(storeOrders, 6));

        // Category revenue
        dto.setCategoryRevenue(buildCategorySpend(storeOrders));

        // Inventory status
        List<Inventory> inventories = inventoryRepository.findByStoreId(storeId);
        List<InventoryStatusDTO> inventoryStatusList = new ArrayList<>();
        for (Inventory inv : inventories) {
            Product p = inv.getProduct();
            if (p != null) {
                inventoryStatusList.add(new InventoryStatusDTO(
                        p.getId(), p.getName(), inv.getStock(), inv.getStock() < 10
                ));
            }
        }
        dto.setInventoryStatus(inventoryStatusList);

        // Membership distribution from customers who ordered from this store
        Set<Long> customerUserIds = storeOrders.stream()
                .map(o -> o.getUser().getId())
                .collect(Collectors.toSet());
        Map<String, Long> membershipDist = new LinkedHashMap<>();
        for (MembershipType mt : MembershipType.values()) {
            membershipDist.put(mt.name(), 0L);
        }
        for (Long custId : customerUserIds) {
            userRepository.findById(custId).ifPresent(u -> {
                individualCustomerRepository.findByUser(u).ifPresent(ic -> {
                    if (ic.getMembershipType() != null) {
                        membershipDist.merge(ic.getMembershipType().name(), 1L, Long::sum);
                    }
                });
            });
        }
        dto.setMembershipDist(membershipDist);

        // Top products (by sold count)
        List<TopProductDTO> topProducts = storeProducts.stream()
                .sorted(Comparator.comparingInt(Product::getSoldCount).reversed())
                .limit(10)
                .map(p -> new TopProductDTO(p.getName(), p.getSoldCount(), p.getPrice().doubleValue() * p.getSoldCount()))
                .collect(Collectors.toList());
        dto.setTopProducts(topProducts);

        // Average fulfillment days
        double avgDays = storeOrders.stream()
                .filter(o -> o.getShipment() != null && o.getShipment().getDeliveryDate() != null && o.getShipment().getShipmentDate() != null)
                .mapToLong(o -> ChronoUnit.DAYS.between(o.getShipment().getShipmentDate(), o.getShipment().getDeliveryDate()))
                .average()
                .orElse(0.0);
        dto.setAvgFulfillmentDays(Math.round(avgDays * 10.0) / 10.0);

        // Satisfaction distribution
        Map<String, Long> satisfactionDist = new LinkedHashMap<>();
        for (SatisfactionLevel sl : SatisfactionLevel.values()) {
            satisfactionDist.put(sl.name(), 0L);
        }
        for (Long custId : customerUserIds) {
            userRepository.findById(custId).ifPresent(u -> {
                individualCustomerRepository.findByUser(u).ifPresent(ic -> {
                    if (ic.getSatisfactionLevel() != null) {
                        satisfactionDist.merge(ic.getSatisfactionLevel().name(), 1L, Long::sum);
                    }
                });
            });
        }
        dto.setSatisfactionDist(satisfactionDist);

        return dto;
    }

    // ─── ADMIN ANALYTICS ──────────────────────────────────

    public AdminAnalyticsDTO getAdminAnalytics() {
        AdminAnalyticsDTO dto = new AdminAnalyticsDTO();

        // Cards
        BigDecimal platformRevenue = orderRepository.sumAllGrandTotal();
        dto.setPlatformRevenue(platformRevenue.doubleValue());

        dto.setActiveStores(storeRepository.countActiveStores());

        Map<String, Long> usersByRole = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            usersByRole.put(role.name(), userRepository.countByRole(role));
        }
        dto.setUsersByRole(usersByRole);

        // Average daily orders (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentOrders = orderRepository.countOrdersSince(thirtyDaysAgo);
        dto.setAvgDailyOrders(Math.round(recentOrders / 30.0 * 100.0) / 100.0);

        Double avgRating = productRepository.averageRating();
        dto.setPlatformAvgRating(avgRating != null ? Math.round(avgRating * 100.0) / 100.0 : 0.0);

        // Pending store approvals — stores with zero revenue as proxy
        dto.setPendingStoreApprovals(storeRepository.count() - storeRepository.countActiveStores());

        // Platform revenue series (last 12 months)
        List<Order> allOrders = orderRepository.findAll();
        dto.setPlatformRevenueSeries(buildMonthlySpend(allOrders, 12));

        // Top 10 stores
        List<Store> allStores = storeRepository.findAll();
        List<StoreComparisonDTO> topStores = allStores.stream()
                .sorted(Comparator.comparing(Store::getTotalRevenue, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(s -> {
                    long orderCount = orderRepository.findAllByStoreId(s.getId()).size();
                    return new StoreComparisonDTO(
                            s.getStoreName(),
                            s.getTotalRevenue() != null ? s.getTotalRevenue().doubleValue() : 0,
                            orderCount
                    );
                })
                .collect(Collectors.toList());
        dto.setTopStores(topStores);

        // Registration trend (last 12 months)
        List<User> allUsers = userRepository.findAll();
        dto.setRegistrationTrend(buildRegistrationTrend(allUsers, 12));

        // Category performance
        dto.setCategoryPerformance(buildCategorySpend(allOrders));

        // Recent audit logs (last 20)
        var auditPage = auditLogRepository.findAll(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<AuditLogEntryDTO> auditEntries = auditPage.getContent().stream()
                .map(a -> new AuditLogEntryDTO(
                        a.getUserEmail(),
                        a.getAction() != null ? a.getAction().name() : "",
                        a.getCreatedAt(),
                        a.getIpAddress()
                ))
                .collect(Collectors.toList());
        dto.setRecentAuditLogs(auditEntries);

        return dto;
    }

    // ─── HELPER METHODS ───────────────────────────────────

    private List<MonthlyAmountDTO> buildMonthlySpend(List<Order> orders, int months) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth now = YearMonth.now();
        Map<String, Double> monthMap = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            monthMap.put(now.minusMonths(i).format(fmt), 0.0);
        }
        for (Order o : orders) {
            if (o.getCreatedAt() != null && o.getGrandTotal() != null) {
                String key = YearMonth.from(o.getCreatedAt()).format(fmt);
                monthMap.computeIfPresent(key, (k, v) -> v + o.getGrandTotal().doubleValue());
            }
        }
        return monthMap.entrySet().stream()
                .map(e -> new MonthlyAmountDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private Map<String, Long> buildOrderStatusDist(List<Order> orders) {
        Map<String, Long> dist = new LinkedHashMap<>();
        for (Order o : orders) {
            String status = "PENDING";
            if (o.getShipment() != null && o.getShipment().getStatus() != null) {
                status = o.getShipment().getStatus().name();
            } else if (o.getPayment() != null && o.getPayment().getPaymentStatus() != null) {
                status = o.getPayment().getPaymentStatus().name();
            }
            dist.merge(status, 1L, Long::sum);
        }
        return dist;
    }

    private List<CategoryAmountDTO> buildCategorySpend(List<Order> orders) {
        Map<String, Double> catMap = new LinkedHashMap<>();
        for (Order o : orders) {
            if (o.getOrderItems() != null) {
                for (OrderItem oi : o.getOrderItems()) {
                    if (oi.getProduct() != null && oi.getProduct().getCategory() != null) {
                        String catName = oi.getProduct().getCategory().getName();
                        double lineTotal = oi.getPrice() != null && oi.getQuantity() != null
                                ? oi.getPrice().doubleValue() * oi.getQuantity()
                                : 0;
                        catMap.merge(catName, lineTotal, Double::sum);
                    }
                }
            }
        }
        return catMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> new CategoryAmountDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<RecentOrderDTO> buildRecentOrders(List<Order> orders, int limit) {
        return orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(o -> {
                    int itemCount = o.getOrderItems() != null ? o.getOrderItems().size() : 0;
                    String status = "PENDING";
                    if (o.getShipment() != null && o.getShipment().getStatus() != null) {
                        status = o.getShipment().getStatus().name();
                    }
                    return new RecentOrderDTO(o.getId(), o.getCreatedAt(), itemCount, o.getGrandTotal(), status);
                })
                .collect(Collectors.toList());
    }

    private List<RegistrationTrendDTO> buildRegistrationTrend(List<User> users, int months) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth now = YearMonth.now();
        Map<String, long[]> monthMap = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            monthMap.put(now.minusMonths(i).format(fmt), new long[]{0, 0});
        }
        for (User u : users) {
            if (u.getCreatedAt() != null) {
                LocalDate ld = new java.sql.Date(u.getCreatedAt().getTime()).toLocalDate();
                String key = YearMonth.from(ld).format(fmt);
                long[] counts = monthMap.get(key);
                if (counts != null && u.getRole() != null) {
                    if (u.getRole() == Role.INDIVIDUAL) counts[0]++;
                    else if (u.getRole() == Role.CORPORATE) counts[1]++;
                }
            }
        }
        return monthMap.entrySet().stream()
                .map(e -> new RegistrationTrendDTO(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());
    }
}
