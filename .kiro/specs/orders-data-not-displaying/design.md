# Orders Data Not Displaying — Bugfix Design

## Overview

Orders are not visible to either user type: Individual users see an empty orders table at `/orders`, and Corporate users see an empty store orders table at `/store-orders`. The bug spans four distinct failure points across the backend and frontend. The fix is targeted at each root cause with no changes to unaffected code paths.

## Glossary

- **Bug_Condition (C)**: The set of conditions that cause orders to not display — covering both the Individual and Corporate user flows.
- **Property (P)**: The desired outcome when the bug condition holds — orders are fetched and rendered correctly.
- **Preservation**: All existing behaviors (pagination, sorting, status updates, CSV export, role guards, product endpoints) that must remain unchanged after the fix.
- **`OrderRepository.findByStoreId`**: The JPQL query in `OrderRepository.java` that fetches orders for a store by joining on `orderItems`. Currently missing `DISTINCT`, causing duplicate rows and broken pagination.
- **`StoreController.getMyStore`**: The endpoint `GET /api/stores/my-store` in `StoreController.java`. Currently returns `{ "error": "No store found" }` with HTTP 200 when no store is found, making it indistinguishable from a valid store response.
- **`StoreOrders.ngOnInit`**: The Angular lifecycle hook in `store-orders.ts` that resolves the store ID before calling `loadOrders()`. Currently does not set `isLoading = false` when the store response has no valid `id`.
- **`Orders.loadOrders`**: The Angular method in `orders.ts` that fetches orders using `auth.getUser().id`. Relies on the session user ID matching the database user ID.
- **`auth.getUser()`**: The `AuthService` method that returns the currently authenticated user from the session/JWT. The `id` field must match the database primary key used in `OrderRepository.findByUser`.

## Bug Details

### Bug Condition

The bug manifests across four independent failure points. Any one of them is sufficient to cause orders not to display.

**Formal Specification:**

```
FUNCTION isBugCondition(X)
  INPUT: X of type { userId: Long, userType: "INDIVIDUAL" | "CORPORATE" }
  OUTPUT: boolean

  IF X.userType = "INDIVIDUAL" THEN
    RETURN auth.getUser().id DOES NOT MATCH database user ID for X.userId
           OR orderRepository.findByUser returns empty due to ID mismatch
  END IF

  IF X.userType = "CORPORATE" THEN
    storeResult ← GET /api/stores/my-store?userId=X.userId
    RETURN storeResult.id IS NULL
           OR storeResult.id IS UNDEFINED
           OR (storeResult has "error" key AND HTTP status is 200)
           OR (storeId is valid BUT findByStoreId returns duplicates causing wrong pagination)
  END IF
END FUNCTION
```

### Examples

- Individual user with `userId=5` navigates to `/orders` → `auth.getUser().id` returns `5`, backend queries `findByUser(user)` but user lookup fails or returns wrong user → empty table displayed.
- Corporate user navigates to `/store-orders` → `GET /api/stores/my-store?userId=3` returns `{ "error": "No store found" }` with HTTP 200 → frontend checks `store.id` which is `undefined` → `isLoading` stays `true` forever, orders never load.
- Corporate user with a valid store → `findByStoreId` joins `orderItems` without `DISTINCT` → an order with 3 items appears 3 times → `totalElements` is inflated → pagination is broken and some pages appear empty.
- Corporate user with a valid store and a single-item order → `findByStoreId` returns correct count → orders display correctly (non-buggy case, must be preserved).

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Pagination, sorting by column, and page size selection on the Orders page must continue to work exactly as before.
- The Ship and Reject status update actions on the Store Orders page must continue to work.
- CSV export of store orders must continue to work.
- Product endpoints (`/products`, `/products/{id}`, etc.) must continue to return correct data.
- Unauthenticated users navigating to `/orders` or `/store-orders` must continue to be redirected to the access-denied page via the role guard.
- `GET /api/stores/my-store` called with a valid Corporate user ID that owns a store must continue to return the full store entity including `id`, `storeName`, and owner details.

**Scope:**
All inputs that do NOT trigger the bug condition — valid store lookups, authenticated users with matching IDs, orders with single items — must be completely unaffected by this fix.

## Hypothesized Root Cause

1. **Missing DISTINCT in JPQL query** (`OrderRepository.findByStoreId`): The query `SELECT o FROM Order o JOIN o.orderItems oi WHERE oi.product.store.id = :storeId` produces one row per matching `OrderItem`, not per `Order`. An order with N items appears N times. Spring Data's `Page` counts these duplicates, so `totalElements` is inflated and pagination breaks. Fix: add `DISTINCT` → `SELECT DISTINCT o FROM Order o JOIN o.orderItems oi WHERE oi.product.store.id = :storeId`.

2. **HTTP 200 on store-not-found** (`StoreController.getMyStore`): When no store is found for the given `userId`, the endpoint returns `ResponseEntity.ok(Map.of("error", "No store found"))` — an HTTP 200 with an error body. The frontend checks `store.id`, which is `undefined` on this response, so it falls into the `else` branch and sets `isLoading = false` — but only if that branch exists. Fix: return `ResponseEntity.notFound().build()` (HTTP 404) so the frontend `error` callback fires.

3. **`isLoading` never reset on invalid store response** (`StoreOrders.ngOnInit`): When `store.id` is falsy, the `else` branch sets `isLoading = false`. This is actually present in the current code. However, if the backend returns HTTP 200 with `{ "error": "..." }`, the `next` callback fires and `store.id` is `undefined`, so `isLoading = false` is set correctly — but `loadOrders()` is never called. The real issue is that the backend returns 200 instead of 4xx, so the `error` callback never fires in the HTTP error case. After fixing the backend to return 404, the frontend `error` callback will correctly set `isLoading = false`.

4. **Session user ID mismatch** (`Orders.loadOrders`): `auth.getUser().id` must match the database primary key. If the JWT stores a different identifier (e.g., email or a stale ID), `userRepository.findById(userId)` will throw or return the wrong user. Fix: verify that `AuthService.getUser()` returns the correct database `id` field from the JWT claims, and that the JWT is issued with the correct `id` on login/registration.

## Correctness Properties

Property 1: Bug Condition — Individual Orders Are Returned

_For any_ authenticated Individual user where `auth.getUser().id` matches a valid database user ID and that user has existing orders, the fixed `GET /api/orders?userId={id}` endpoint SHALL return a non-empty paginated result with `totalElements > 0` and `content` containing the user's orders.

**Validates: Requirements 2.1, 2.3**

Property 2: Bug Condition — Store Orders Are Returned

_For any_ authenticated Corporate user where `GET /api/stores/my-store?userId={id}` returns a store with a valid `id`, the fixed `GET /api/store-orders?storeId={id}` endpoint SHALL return DISTINCT orders (each order appearing exactly once regardless of how many order items it contains), with correct `totalElements` and non-empty `content` when orders exist.

**Validates: Requirements 2.2, 2.4, 2.6**

Property 3: Bug Condition — Store Not Found Handled Correctly

_For any_ Corporate user where no store exists for their `userId`, the fixed `GET /api/stores/my-store?userId={id}` endpoint SHALL return HTTP 404, and the frontend SHALL set `isLoading = false` and display an appropriate empty state without leaving the UI in a perpetual loading state.

**Validates: Requirements 2.5**

Property 4: Preservation — Non-Buggy Inputs Unchanged

_For any_ input where the bug condition does NOT hold (valid store lookup, correct user ID, single-item orders), the fixed code SHALL produce the same result as the original code, preserving pagination, sorting, status updates, CSV export, role guards, and product endpoint behavior.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

## Fix Implementation

### Changes Required

**File 1**: `backend/src/main/java/com/shop/ecommerce/repository/OrderRepository.java`

**Change**: Add `DISTINCT` to `findByStoreId` query.

```java
// Before
@Query("SELECT o FROM Order o JOIN o.orderItems oi WHERE oi.product.store.id = :storeId")

// After
@Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi WHERE oi.product.store.id = :storeId")
```

---

**File 2**: `backend/src/main/java/com/shop/ecommerce/controller/StoreController.java`

**Change**: Return HTTP 404 instead of HTTP 200 with an error body when no store is found.

```java
// Before
return ResponseEntity.ok(Map.of("error", "No store found"));

// After
return ResponseEntity.notFound().build();
```

---

**File 3**: `frontend/src/app/components/pages/store-orders/store-orders.ts`

**Change**: The `error` callback already sets `isLoading = false`. After the backend fix (returning 404), the `error` callback will fire correctly. No frontend change is strictly required for the store-not-found case once the backend is fixed. However, add a defensive guard to ensure `isLoading` is always reset even if the response shape is unexpected:

```typescript
// In ngOnInit next callback — already present, confirm it handles all cases:
next: (store: any) => {
  if (store && store.id) {
    this.storeId = store.id;
    this.loadOrders();
  } else {
    this.isLoading = false; // already present — preserved
  }
},
error: () => { this.isLoading = false; } // fires on 404 after backend fix
```

No code change needed here if the backend fix is applied. The existing `else { this.isLoading = false; }` branch handles the case where a 200 with no `id` is returned, and the `error` callback handles 4xx responses.

---

**File 4**: `frontend/src/app/core/services/auth.service.ts` / JWT issuance

**Change**: Verify that `AuthService.getUser()` returns the correct database `id`. Inspect the JWT payload to confirm the `id` claim matches `User.id` in the database. If the JWT is issued with a different field (e.g., `sub` = email), update either the JWT issuance in `AuthController`/`AuthService` to include the numeric `id`, or update `Orders.loadOrders` to use the correct field.

This requires investigation during the exploratory phase to confirm whether a mismatch actually exists.

## Testing Strategy

### Validation Approach

Testing follows a two-phase approach: first run exploratory tests on unfixed code to confirm root causes, then verify the fix and preservation after applying changes.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate each bug on unfixed code. Confirm or refute the root cause analysis.

**Test Plan**: Write integration/unit tests that exercise each failure point against the unfixed code and assert the buggy behavior.

**Test Cases**:
1. **Duplicate Orders Test**: Call `findByStoreId` with a store that has an order containing 3 items. Assert that the returned `Page` contains 3 entries instead of 1 (demonstrates the missing DISTINCT bug). (will fail on fixed code — expected to pass on unfixed code)
2. **Store Not Found HTTP Status Test**: Call `GET /api/stores/my-store?userId={nonExistentUserId}` and assert the response status is 200 with body `{ "error": "No store found" }`. (demonstrates the wrong HTTP status bug)
3. **Frontend isLoading Test**: Simulate `ngOnInit` with a mock HTTP response of `{ "error": "No store found" }` (HTTP 200). Assert that `isLoading` is set to `false` and `loadOrders` is not called. (demonstrates the frontend loading state issue)
4. **User ID Mismatch Test**: Call `GET /api/orders?userId={id}` where `id` does not match any database user. Assert the response is an error or empty, not a 500 crash.

**Expected Counterexamples**:
- `findByStoreId` returns N rows for an order with N items instead of 1 distinct order.
- `GET /api/stores/my-store` returns HTTP 200 with `{ "error": "No store found" }` instead of HTTP 404.

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed functions produce the expected behavior.

**Pseudocode:**
```
FOR ALL X WHERE isBugCondition(X) DO
  result := fixedFunction(X)
  ASSERT expectedBehavior(result)
END FOR
```

**Specific assertions after fix:**
- `findByStoreId` for a store with a 3-item order returns exactly 1 distinct order.
- `GET /api/stores/my-store?userId={noStore}` returns HTTP 404.
- `GET /api/orders?userId={validId}` returns non-empty `content` when orders exist.
- Frontend `StoreOrders.ngOnInit` with a 404 response sets `isLoading = false`.

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed functions produce the same result as the original.

**Pseudocode:**
```
FOR ALL X WHERE NOT isBugCondition(X) DO
  ASSERT originalFunction(X) = fixedFunction(X)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because it generates many test cases automatically, catches edge cases, and provides strong guarantees across the input domain.

**Test Cases**:
1. **Valid Store Lookup Preservation**: `GET /api/stores/my-store?userId={validCorporateUser}` continues to return the full store entity with `id`, `storeName`, and owner details — unchanged by the 404 fix.
2. **Single-Item Order Preservation**: `findByStoreId` for a store with single-item orders returns the same results before and after adding `DISTINCT`.
3. **Pagination Preservation**: Orders page with valid user ID continues to support page/size/sort parameters correctly.
4. **Status Update Preservation**: `PATCH /api/store-orders/{orderId}/status` continues to update shipment status correctly.
5. **CSV Export Preservation**: `exportCsv()` in `StoreOrders` continues to generate correct CSV output.
6. **Role Guard Preservation**: Unauthenticated navigation to `/orders` and `/store-orders` continues to redirect to access-denied.

### Unit Tests

- Test `OrderRepository.findByStoreId` with a store having multi-item orders — assert DISTINCT behavior.
- Test `StoreController.getMyStore` with a non-existent user — assert HTTP 404 response.
- Test `StoreController.getMyStore` with a valid user owning a store — assert HTTP 200 with full store entity.
- Test `Orders.loadOrders` with a mocked `auth.getUser()` returning a valid user — assert HTTP call is made with correct `userId`.
- Test `StoreOrders.ngOnInit` with a mocked 404 response — assert `isLoading = false` and `loadOrders` not called.

### Property-Based Tests

- Generate random stores with varying numbers of order items per order; verify `findByStoreId` always returns exactly one entry per order regardless of item count.
- Generate random valid user IDs; verify `GET /api/orders?userId={id}` always returns a well-formed paginated response (never a 500).
- Generate random pagination parameters (page, size, sortBy, sortDir); verify the Orders and StoreOrders endpoints return consistent results before and after the fix.

### Integration Tests

- Full flow: Corporate user logs in → `GET /api/stores/my-store` returns store → `GET /api/store-orders?storeId={id}` returns orders → table renders with correct row count.
- Full flow: Individual user logs in → `GET /api/orders?userId={id}` returns orders → table renders with correct row count.
- Store not found flow: Corporate user with no store → `GET /api/stores/my-store` returns 404 → frontend shows empty state, not infinite loading.
- Multi-item order flow: Store with orders containing multiple items → store orders table shows each order exactly once with correct total.
