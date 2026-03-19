# Implementation Plan

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Orders Not Displaying (Duplicate Rows + Wrong HTTP Status)
  - **CRITICAL**: This test MUST FAIL on unfixed code — failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior — it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate each of the four failure points
  - **Scoped PBT Approach**: Scope to concrete failing cases for reproducibility
  - Test 1a — Duplicate Orders: Call `OrderRepository.findByStoreId` with a store that has one order containing 3 items. Assert the returned `Page` contains exactly 1 entry (not 3). Run on UNFIXED code — expect FAILURE (returns 3 duplicates).
  - Test 1b — Wrong HTTP Status: Call `GET /api/stores/my-store?userId={nonExistentUserId}`. Assert response status is 404. Run on UNFIXED code — expect FAILURE (returns HTTP 200 with `{ "error": "No store found" }`).
  - Test 1c — Frontend isLoading: Simulate `StoreOrders.ngOnInit` with a mock HTTP 200 response of `{ "error": "No store found" }`. Assert `isLoading` is `false` and `loadOrders` is not called.
  - Test 1d — User ID: Call `GET /api/orders?userId={id}` where `auth.getUser().id` matches the database user ID and orders exist. Assert `content` is non-empty.
  - Document counterexamples found (e.g., `findByStoreId` returns 3 rows for a 3-item order; `GET /api/stores/my-store` returns HTTP 200 with error body)
  - Mark task complete when tests are written, run, and failures are documented
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Non-Buggy Inputs Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Observe: `GET /api/stores/my-store?userId={validCorporateUser}` returns full store entity with `id`, `storeName`, and owner details on unfixed code
  - Observe: `findByStoreId` for a store with single-item orders returns the same count before and after adding `DISTINCT`
  - Observe: Pagination, sorting, status updates, CSV export, and role guards all work correctly on unfixed code for non-buggy inputs
  - Write property-based test: for all valid Corporate users owning a store, `GET /api/stores/my-store` returns HTTP 200 with a non-null `id` field (from Preservation Requirements 3.6)
  - Write property-based test: for all stores with single-item orders, `findByStoreId` returns the same result with or without `DISTINCT` (from Preservation Requirements 3.1)
  - Write property-based test: for all valid pagination parameters (page, size, sortBy, sortDir), the Orders and StoreOrders endpoints return consistent well-formed paginated responses (from Preservation Requirements 3.1)
  - Verify all preservation tests PASS on UNFIXED code before proceeding
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [ ] 3. Fix for orders data not displaying

  - [x] 3.1 Add DISTINCT to `OrderRepository.findByStoreId` JPQL query
    - In `backend/src/main/java/com/shop/ecommerce/repository/OrderRepository.java`, change the `@Query` annotation on `findByStoreId` from `SELECT o FROM Order o JOIN o.orderItems oi WHERE oi.product.store.id = :storeId` to `SELECT DISTINCT o FROM Order o JOIN o.orderItems oi WHERE oi.product.store.id = :storeId`
    - _Bug_Condition: isBugCondition_StoreOrders(X) where findByStoreId returns N rows for an order with N items_
    - _Expected_Behavior: findByStoreId returns exactly one row per order regardless of item count_
    - _Preservation: Single-item order results must be identical before and after adding DISTINCT (Requirements 3.1)_
    - _Requirements: 1.6, 2.6_

  - [x] 3.2 Return HTTP 404 from `StoreController.getMyStore` when no store is found
    - In `backend/src/main/java/com/shop/ecommerce/controller/StoreController.java`, replace `return ResponseEntity.ok(Map.of("error", "No store found"))` with `return ResponseEntity.notFound().build()`
    - _Bug_Condition: isBugCondition_StoreOrders(X) where storeResult.id IS NULL because HTTP 200 error body is returned_
    - _Expected_Behavior: GET /api/stores/my-store returns HTTP 404 when no store exists for the given userId_
    - _Preservation: Valid store lookups must still return HTTP 200 with full store entity (Requirements 3.6)_
    - _Requirements: 1.4, 2.4, 2.5_

  - [x] 3.3 Verify `StoreOrders.ngOnInit` correctly handles 404 response
    - In `frontend/src/app/components/pages/store-orders/store-orders.ts`, confirm the `error` callback sets `isLoading = false`
    - Confirm the `next` callback has an `else { this.isLoading = false; }` branch for responses with no valid `id`
    - No code change is required if both branches are already present — document the verification result
    - _Bug_Condition: isLoading stays true forever when store-not-found response arrives_
    - _Expected_Behavior: isLoading = false is set in both the error callback (404) and the else branch (200 with no id)_
    - _Preservation: loadOrders() must still be called when a valid store id is returned (Requirements 3.2, 3.3)_
    - _Requirements: 1.5, 2.5_

  - [x] 3.4 Verify `AuthService.getUser()` returns the correct database `id` from JWT claims
    - In `frontend/src/app/core/services/auth.service.ts`, inspect the JWT payload to confirm the `id` claim matches `User.id` in the database
    - In `backend/src/main/java/com/shop/ecommerce/controller/AuthController.java` (or the JWT issuance service), confirm the numeric database `id` is included in the JWT claims at login/registration
    - If a mismatch is found, update JWT issuance to include the correct `id` field, or update `Orders.loadOrders` to use the correct claim field
    - _Bug_Condition: auth.getUser().id does not match database user ID, causing findByUser to return empty results_
    - _Expected_Behavior: auth.getUser().id equals the database primary key used in OrderRepository.findByUser_
    - _Preservation: All other JWT claims and auth flows must remain unchanged (Requirements 3.5)_
    - _Requirements: 1.3, 2.1, 2.3_

  - [x] 3.5 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Orders Not Displaying (All Four Failure Points Fixed)
    - **IMPORTANT**: Re-run the SAME tests from task 1 — do NOT write new tests
    - The tests from task 1 encode the expected behavior
    - Run all four sub-tests (1a duplicate orders, 1b HTTP status, 1c frontend isLoading, 1d user ID)
    - **EXPECTED OUTCOME**: All tests PASS (confirms all four bugs are fixed)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [ ] 3.6 Verify preservation tests still pass
    - **Property 2: Preservation** - Non-Buggy Inputs Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 — do NOT write new tests
    - Run all preservation property tests from step 2
    - **EXPECTED OUTCOME**: All tests PASS (confirms no regressions in pagination, sorting, status updates, CSV export, role guards, valid store lookups)
    - Confirm all tests still pass after fix (no regressions)

- [ ] 4. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
