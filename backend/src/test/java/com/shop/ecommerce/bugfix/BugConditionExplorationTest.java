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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bug Condition Exploration Tests — Task 1
 *
 * These tests are EXPECTED TO FAIL on unfixed code.
 * Failure confirms the bugs exist. DO NOT fix the code to make these pass.
 *
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BugConditionExplorationTest {

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
        // Clean up before each test
        orderRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();

        // Create a corporate user who owns a store
        storeOwner = new User();
        storeOwner.setName("Store");
        storeOwner.setSurname("Owner");
        storeOwner.setEmail("storeowner@test.com");
        storeOwner.setPassword("password");
        storeOwner.setPhone("1111111111");
        storeOwner.setRole(Role.CORPORATE);
        storeOwner.setCreatedAt(new Date());
        storeOwner = userRepository.save(storeOwner);

        store = new Store();
        store.setStoreName("Test Store");
        store.setDescription("A test store");
        store.setCompanyName("Test Co");
        store.setTaxNumber("TAX123");
        store.setTaxOffice("Test Office");
        store.setOwner(storeOwner);
        store = storeRepository.save(store);

        // Create an individual user with orders
        individualUser = new User();
        individualUser.setName("Individual");
        individualUser.setSurname("User");
        individualUser.setEmail("individual@test.com");
        individualUser.setPassword("password");
        individualUser.setPhone("2222222222");
        individualUser.setRole(Role.INDIVIDUAL);
        individualUser.setCreatedAt(new Date());
        individualUser = userRepository.save(individualUser);
    }

    /**
     * Test 1a — Duplicate Orders
     *
     * Bug: findByStoreId JPQL query is missing DISTINCT, causing one row per OrderItem.
     * An order with 3 items appears 3 times in the result.
     *
     * EXPECTED TO FAIL on unfixed code (PostgreSQL): returns 3 rows instead of 1.
     * NOTE: H2 may deduplicate at the entity level, so this test may pass on H2.
     * The bug is confirmed on PostgreSQL where totalElements = 3 for a 3-item order.
     *
     * Validates: Requirements 1.6
     */
    @Test
    void test1a_findByStoreId_shouldReturnExactlyOneOrderForThreeItemOrder() {
        // Arrange: create one order with 3 items, all from the same store
        Product p1 = createProduct("Product A", store);
        Product p2 = createProduct("Product B", store);
        Product p3 = createProduct("Product C", store);

        Order order = new Order();
        order.setUser(individualUser);
        order.setGrandTotal(new BigDecimal("300.00"));
        order.setShippingAddress("123 Test St");
        order = orderRepository.save(order);

        OrderItem item1 = new OrderItem(null, order, p1, new BigDecimal("100.00"), 1);
        OrderItem item2 = new OrderItem(null, order, p2, new BigDecimal("100.00"), 1);
        OrderItem item3 = new OrderItem(null, order, p3, new BigDecimal("100.00"), 1);
        order.getOrderItems().add(item1);
        order.getOrderItems().add(item2);
        order.getOrderItems().add(item3);
        orderRepository.save(order);

        // Act
        Page<Order> result = orderRepository.findByStoreId(store.getId(), PageRequest.of(0, 10));

        // Assert: exactly 1 distinct order should be returned
        // BUG (PostgreSQL): without DISTINCT, totalElements = 3 (one per item)
        // H2 may deduplicate at entity level — the bug is confirmed on PostgreSQL
        assertThat(result.getTotalElements())
                .as("findByStoreId should return exactly 1 order, not 3 duplicates (missing DISTINCT bug). "
                        + "Actual totalElements=" + result.getTotalElements()
                        + ", content size=" + result.getContent().size())
                .isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Test 1b — Wrong HTTP Status
     *
     * Bug: GET /api/stores/my-store?userId={nonExistentUserId} returns HTTP 200
     * with body {"error": "No store found"} instead of HTTP 404.
     *
     * EXPECTED TO FAIL on unfixed code: returns HTTP 200 instead of 404.
     *
     * Validates: Requirements 1.4
     */
    @Test
    void test1b_getMyStore_shouldReturn404WhenNoStoreFound() throws Exception {
        // Use the individual user who has no store
        Long userWithNoStore = individualUser.getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/stores/my-store?userId=" + userWithNoStore))
                .GET()
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Assert: expect 404 when no store exists for this user
        // BUG: unfixed code returns HTTP 200 with {"error": "No store found"} — test FAILS
        assertThat(response.statusCode())
                .as("GET /api/stores/my-store should return 404 when no store found, not 200 with error body. "
                        + "Actual response body: " + response.body())
                .isEqualTo(404);
    }

    /**
     * Test 1d — User ID
     *
     * Validates that GET /api/orders?userId={id} returns non-empty content
     * when auth.getUser().id matches the database user ID and orders exist.
     *
     * EXPECTED TO FAIL if auth.getUser().id does not match the database user ID.
     * On a correctly configured system this should pass; failure indicates ID mismatch.
     *
     * Validates: Requirements 1.3, 2.1, 2.3
     */
    @Test
    void test1d_getOrders_shouldReturnNonEmptyContentForValidUserId() throws Exception {
        // Arrange: create an order for the individual user
        Order order = new Order();
        order.setUser(individualUser);
        order.setGrandTotal(new BigDecimal("50.00"));
        order.setShippingAddress("456 Test Ave");
        orderRepository.save(order);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/orders?userId=" + individualUser.getId()))
                .GET()
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Assert: GET /api/orders?userId={id} should return 200 with non-empty content
        assertThat(response.statusCode())
                .as("GET /api/orders should return 200")
                .isEqualTo(200);

        String body = response.body();
        assertThat(body).contains("\"content\"");

        // The content array must not be empty
        // BUG: if auth.getUser().id doesn't match DB user ID, returns empty — test FAILS
        assertThat(body)
                .as("content should not be empty — orders exist for this user. Body: " + body)
                .doesNotContain("\"content\":[]");
    }

    // ---- helpers ----

    private Product createProduct(String name, Store store) {
        Product p = new Product();
        p.setName(name);
        p.setDescription("desc");
        p.setPrice(100.0);
        p.setStore(store);
        return productRepository.save(p);
    }
}
