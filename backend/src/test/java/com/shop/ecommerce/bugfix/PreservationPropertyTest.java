package com.shop.ecommerce.bugfix;

import com.shop.ecommerce.entities.*;
import com.shop.ecommerce.enums.Role;
import com.shop.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Preservation Property Tests — Task 2
 *
 * These tests verify that non-buggy inputs are completely unaffected by the fix.
 * They MUST PASS on UNFIXED code — they establish the preservation baseline.
 *
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PreservationPropertyTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    private User storeOwner;
    private Store store;
    private User individualUser;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();

        // Corporate user who owns a store (non-buggy: store exists)
        storeOwner = new User();
        storeOwner.setName("Store");
        storeOwner.setSurname("Owner");
        storeOwner.setEmail("storeowner@preservation.com");
        storeOwner.setPassword("password");
        storeOwner.setPhone("3333333333");
        storeOwner.setRole(Role.CORPORATE);
        storeOwner.setCreatedAt(new Date());
        storeOwner = userRepository.save(storeOwner);

        store = new Store();
        store.setStoreName("Preservation Store");
        store.setDescription("A store for preservation tests");
        store.setCompanyName("Preservation Co");
        store.setTaxNumber("TAX999");
        store.setTaxOffice("Preservation Office");
        store.setOwner(storeOwner);
        store = storeRepository.save(store);

        // Individual user
        individualUser = new User();
        individualUser.setName("Individual");
        individualUser.setSurname("Preservation");
        individualUser.setEmail("individual@preservation.com");
        individualUser.setPassword("password");
        individualUser.setPhone("4444444444");
        individualUser.setRole(Role.INDIVIDUAL);
        individualUser.setCreatedAt(new Date());
        individualUser = userRepository.save(individualUser);
    }

    // -------------------------------------------------------------------------
    // Property P3.6 — Valid store lookup returns full entity (Req 3.6)
    // -------------------------------------------------------------------------

    /**
     * Property: for all valid Corporate users owning a store,
     * GET /api/stores/my-store returns HTTP 200 with a non-null id field.
     *
     * This must pass on UNFIXED code — the fix (returning 404 for missing stores)
     * must NOT affect the happy path where a store exists.
     *
     * Validates: Requirements 3.6
     */
    @Test
    void p3_6_getMyStore_shouldReturn200WithIdForValidCorporateUser() throws Exception {
        // storeOwner has a store — this is the non-buggy case
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/stores/my-store?userId=" + storeOwner.getId()))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode())
                .as("GET /api/stores/my-store with valid owner should return HTTP 200. Body: " + response.body())
                .isEqualTo(200);

        String body = response.body();
        assertThat(body)
                .as("Response body should contain store id field. Body: " + body)
                .contains("\"id\"");
        assertThat(body)
                .as("Response body should contain storeName. Body: " + body)
                .contains("\"storeName\"");
        assertThat(body)
                .as("Response body should not contain error key. Body: " + body)
                .doesNotContain("\"error\"");
    }

    /**
     * Property: for multiple distinct Corporate users each owning their own store,
     * GET /api/stores/my-store returns HTTP 200 with a non-null id for each.
     *
     * Simulates property-based iteration over valid Corporate users.
     *
     * Validates: Requirements 3.6
     */
    @Test
    void p3_6_getMyStore_shouldReturn200WithIdForMultipleValidCorporateUsers() throws Exception {
        // Create additional corporate users with stores
        List<Long> ownerIds = new java.util.ArrayList<>();
        ownerIds.add(storeOwner.getId()); // already created in setUp

        for (int i = 1; i <= 3; i++) {
            User owner = new User();
            owner.setName("Owner" + i);
            owner.setSurname("Test");
            owner.setEmail("owner" + i + "@preservation.com");
            owner.setPassword("password");
            owner.setPhone("555555555" + i);
            owner.setRole(Role.CORPORATE);
            owner.setCreatedAt(new Date());
            owner = userRepository.save(owner);

            Store s = new Store();
            s.setStoreName("Store " + i);
            s.setDescription("desc");
            s.setCompanyName("Co " + i);
            s.setTaxNumber("TAX" + i);
            s.setTaxOffice("Office " + i);
            s.setOwner(owner);
            storeRepository.save(s);

            ownerIds.add(owner.getId());
        }

        // For each valid owner, the endpoint must return 200 with an id
        for (Long ownerId : ownerIds) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/stores/my-store?userId=" + ownerId))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode())
                    .as("GET /api/stores/my-store for ownerId=" + ownerId + " should return 200. Body: " + response.body())
                    .isEqualTo(200);
            assertThat(response.body())
                    .as("Response for ownerId=" + ownerId + " should contain id. Body: " + response.body())
                    .contains("\"id\"");
        }
    }

    // -------------------------------------------------------------------------
    // Property P3.1a — Single-item orders: findByStoreId result unchanged (Req 3.1)
    // -------------------------------------------------------------------------

    /**
     * Property: for all stores with single-item orders,
     * findByStoreId returns exactly 1 result per order (same as with DISTINCT).
     *
     * Adding DISTINCT to the query must not change the result for single-item orders.
     *
     * Validates: Requirements 3.1
     */
    @Test
    void p3_1_findByStoreId_singleItemOrder_returnsExactlyOneResult() {
        // Arrange: one order with exactly one item
        Product product = createProduct("Single Product", store);

        Order order = new Order();
        order.setUser(individualUser);
        order.setGrandTotal(new BigDecimal("50.00"));
        order.setShippingAddress("1 Preservation St");
        order = orderRepository.save(order);

        OrderItem item = new OrderItem(null, order, product, new BigDecimal("50.00"), 1);
        order.getOrderItems().add(item);
        orderRepository.save(order);

        // Act
        Page<Order> result = orderRepository.findByStoreId(store.getId(), PageRequest.of(0, 10));

        // Assert: single-item order must appear exactly once
        assertThat(result.getTotalElements())
                .as("Single-item order should appear exactly once in findByStoreId result")
                .isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(order.getId());
    }

    /**
     * Property: for multiple stores each with single-item orders,
     * findByStoreId returns the correct count for each store independently.
     *
     * Simulates property-based iteration over stores with single-item orders.
     *
     * Validates: Requirements 3.1
     */
    @Test
    void p3_1_findByStoreId_multipleStoresWithSingleItemOrders_eachReturnsCorrectCount() {
        // Create 3 additional stores, each with a different number of single-item orders
        int[] orderCounts = {1, 2, 3};

        for (int storeIdx = 0; storeIdx < orderCounts.length; storeIdx++) {
            User owner = new User();
            owner.setName("Owner" + storeIdx);
            owner.setSurname("Multi");
            owner.setEmail("multiowner" + storeIdx + "@preservation.com");
            owner.setPassword("password");
            owner.setPhone("666666666" + storeIdx);
            owner.setRole(Role.CORPORATE);
            owner.setCreatedAt(new Date());
            owner = userRepository.save(owner);

            Store s = new Store();
            s.setStoreName("Multi Store " + storeIdx);
            s.setDescription("desc");
            s.setCompanyName("Multi Co " + storeIdx);
            s.setTaxNumber("MTAX" + storeIdx);
            s.setTaxOffice("Multi Office " + storeIdx);
            s.setOwner(owner);
            s = storeRepository.save(s);

            Product p = createProduct("Product for store " + storeIdx, s);

            int count = orderCounts[storeIdx];
            for (int orderIdx = 0; orderIdx < count; orderIdx++) {
                Order order = new Order();
                order.setUser(individualUser);
                order.setGrandTotal(new BigDecimal("10.00"));
                order.setShippingAddress("Addr " + orderIdx);
                order = orderRepository.save(order);

                OrderItem item = new OrderItem(null, order, p, new BigDecimal("10.00"), 1);
                order.getOrderItems().add(item);
                orderRepository.save(order);
            }

            // Assert: each store returns exactly the number of orders created (no duplicates)
            Page<Order> result = orderRepository.findByStoreId(s.getId(), PageRequest.of(0, 10));
            assertThat(result.getTotalElements())
                    .as("Store " + storeIdx + " with " + count + " single-item orders should return " + count + " results")
                    .isEqualTo((long) count);
        }
    }

    // -------------------------------------------------------------------------
    // Property P3.1b — Pagination parameters produce well-formed responses (Req 3.1)
    // -------------------------------------------------------------------------

    /**
     * Property: for all valid pagination parameters (page, size, sortBy, sortDir),
     * GET /api/orders returns a well-formed paginated response.
     *
     * Validates: Requirements 3.1
     */
    @Test
    void p3_1_ordersEndpoint_validPaginationParams_returnsWellFormedResponse() throws Exception {
        // Arrange: create some orders for the individual user
        for (int i = 0; i < 5; i++) {
            Order order = new Order();
            order.setUser(individualUser);
            order.setGrandTotal(new BigDecimal("10.00").multiply(new BigDecimal(i + 1)));
            order.setShippingAddress("Addr " + i);
            orderRepository.save(order);
        }

        // Test multiple pagination parameter combinations
        int[][] pageSizeCombinations = {{0, 5}, {0, 10}, {1, 2}, {0, 1}};
        String[] sortByOptions = {"createdAt"};
        String[] sortDirOptions = {"asc", "desc"};

        for (int[] ps : pageSizeCombinations) {
            for (String sortBy : sortByOptions) {
                for (String sortDir : sortDirOptions) {
                    String url = "http://localhost:" + port + "/api/orders"
                            + "?userId=" + individualUser.getId()
                            + "&page=" + ps[0]
                            + "&size=" + ps[1]
                            + "&sortBy=" + sortBy
                            + "&sortDir=" + sortDir;

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    assertThat(response.statusCode())
                            .as("GET /api/orders with page=" + ps[0] + " size=" + ps[1]
                                    + " sortBy=" + sortBy + " sortDir=" + sortDir + " should return 200. Body: " + response.body())
                            .isEqualTo(200);

                    String body = response.body();
                    assertThat(body)
                            .as("Response should contain 'content' field. Body: " + body)
                            .contains("\"content\"");
                    assertThat(body)
                            .as("Response should contain 'totalElements' field. Body: " + body)
                            .contains("\"totalElements\"");
                    assertThat(body)
                            .as("Response should contain 'pageable' field. Body: " + body)
                            .contains("\"pageable\"");
                }
            }
        }
    }

    /**
     * Property: for all valid pagination parameters,
     * GET /api/store-orders returns a well-formed paginated response.
     *
     * Validates: Requirements 3.1
     */
    @Test
    void p3_1_storeOrdersEndpoint_validPaginationParams_returnsWellFormedResponse() throws Exception {
        // Arrange: create a single-item order for the store
        Product product = createProduct("Paginated Product", store);

        Order order = new Order();
        order.setUser(individualUser);
        order.setGrandTotal(new BigDecimal("25.00"));
        order.setShippingAddress("Pagination Ave");
        order = orderRepository.save(order);

        OrderItem item = new OrderItem(null, order, product, new BigDecimal("25.00"), 1);
        order.getOrderItems().add(item);
        orderRepository.save(order);

        // Test multiple pagination parameter combinations
        int[][] pageSizeCombinations = {{0, 5}, {0, 10}, {0, 1}};
        String[] sortDirOptions = {"asc", "desc"};

        for (int[] ps : pageSizeCombinations) {
            for (String sortDir : sortDirOptions) {
                String url = "http://localhost:" + port + "/api/store-orders"
                        + "?storeId=" + store.getId()
                        + "&page=" + ps[0]
                        + "&size=" + ps[1]
                        + "&sortBy=createdAt"
                        + "&sortDir=" + sortDir;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                assertThat(response.statusCode())
                        .as("GET /api/store-orders with page=" + ps[0] + " size=" + ps[1]
                                + " sortDir=" + sortDir + " should return 200. Body: " + response.body())
                        .isEqualTo(200);

                String body = response.body();
                assertThat(body)
                        .as("Response should contain 'content' field. Body: " + body)
                        .contains("\"content\"");
                assertThat(body)
                        .as("Response should contain 'totalElements' field. Body: " + body)
                        .contains("\"totalElements\"");
            }
        }
    }

    /**
     * Property: repository-level pagination with Sort is consistent for single-item orders.
     * Verifies that sorting parameters do not break result integrity.
     *
     * Validates: Requirements 3.1
     */
    @Test
    void p3_1_findByStoreId_withSortingParams_returnsConsistentResults() {
        // Arrange: 3 single-item orders
        Product product = createProduct("Sort Product", store);

        for (int i = 0; i < 3; i++) {
            Order order = new Order();
            order.setUser(individualUser);
            order.setGrandTotal(new BigDecimal("10.00").multiply(new BigDecimal(i + 1)));
            order.setShippingAddress("Sort Addr " + i);
            order = orderRepository.save(order);

            OrderItem item = new OrderItem(null, order, product, new BigDecimal("10.00"), 1);
            order.getOrderItems().add(item);
            orderRepository.save(order);
        }

        // Test with different sort directions
        Page<Order> ascResult = orderRepository.findByStoreId(
                store.getId(),
                PageRequest.of(0, 10, Sort.by("id").ascending()));
        Page<Order> descResult = orderRepository.findByStoreId(
                store.getId(),
                PageRequest.of(0, 10, Sort.by("id").descending()));

        // Both should return the same total count
        assertThat(ascResult.getTotalElements())
                .as("Ascending sort should return 3 orders")
                .isEqualTo(3L);
        assertThat(descResult.getTotalElements())
                .as("Descending sort should return 3 orders")
                .isEqualTo(3L);

        // Content should be in opposite order
        assertThat(ascResult.getContent().get(0).getId())
                .as("First element in ascending should be smallest id")
                .isLessThan(ascResult.getContent().get(2).getId());
        assertThat(descResult.getContent().get(0).getId())
                .as("First element in descending should be largest id")
                .isGreaterThan(descResult.getContent().get(2).getId());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Product createProduct(String name, Store store) {
        Product p = new Product();
        p.setName(name);
        p.setDescription("desc");
        p.setPrice(10.0);
        p.setStore(store);
        return productRepository.save(p);
    }
}
