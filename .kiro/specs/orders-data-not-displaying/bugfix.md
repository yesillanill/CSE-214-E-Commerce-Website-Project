# Bugfix Requirements Document

## Introduction

Orders data is not displaying in the frontend tables for both user types:
- **Individual users** cannot see their own order history at `/orders`
- **Corporate users** cannot see their store's incoming orders at `/store-orders`

Both pages render an empty state or fail silently despite orders existing in the database. The bug spans the full request lifecycle — from how the frontend resolves user/store identity, to how the backend queries and returns paginated order data.

---

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN an authenticated Individual user navigates to the Orders page THEN the system displays an empty orders table even when the user has existing orders in the database

1.2 WHEN an authenticated Corporate user navigates to the Store Orders page THEN the system displays an empty orders table even when the store has existing incoming orders in the database

1.3 WHEN the frontend calls `GET /api/orders?userId={id}` with a valid user ID THEN the system may return an empty page result due to a mismatch between the user ID stored in the JWT/session and the actual database user ID used in the query

1.4 WHEN the frontend calls `GET /api/stores/my-store?userId={id}` to resolve the store ID for a Corporate user THEN the system may fail to find the store or return an error object `{ "error": "No store found" }` which the frontend does not handle as a missing-store case, causing `storeId` to remain null and `loadOrders()` to never be called

1.5 WHEN `storeId` is null at the time `loadOrders()` is called in the Store Orders component THEN the system silently returns without fetching any data and sets `isLoading = false` only after a successful store lookup, leaving the UI in a loading or empty state

1.6 WHEN the backend `OrderRepository.findByStoreId` query executes THEN the system may return duplicate orders (one per matching order item) because the JPQL join query `SELECT o FROM Order o JOIN o.orderItems oi WHERE oi.product.store.id = :storeId` does not use DISTINCT, causing inflated result counts and incorrect pagination

### Expected Behavior (Correct)

2.1 WHEN an authenticated Individual user navigates to the Orders page THEN the system SHALL fetch and display all orders associated with that user's account in a paginated table

2.2 WHEN an authenticated Corporate user navigates to the Store Orders page THEN the system SHALL fetch and display all incoming orders containing products from that user's store in a paginated table

2.3 WHEN the frontend calls `GET /api/orders?userId={id}` with a valid user ID THEN the system SHALL return the correct paginated list of orders belonging to that user

2.4 WHEN the frontend calls `GET /api/stores/my-store?userId={id}` and a store is found THEN the system SHALL return the store object with a valid `id` field, and the frontend SHALL use that `id` to load store orders

2.5 WHEN `GET /api/stores/my-store?userId={id}` returns a response without a valid `id` (store not found) THEN the system SHALL set `isLoading = false` and display an appropriate empty state to the user

2.6 WHEN the backend `OrderRepository.findByStoreId` query executes THEN the system SHALL return DISTINCT orders so that each order appears only once regardless of how many matching order items it contains

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a user with existing orders loads the Orders page THEN the system SHALL CONTINUE TO support pagination, sorting by column, and page size selection

3.2 WHEN a Corporate user loads the Store Orders page THEN the system SHALL CONTINUE TO allow updating shipment status (Ship / Reject) for individual orders

3.3 WHEN a Corporate user loads the Store Orders page THEN the system SHALL CONTINUE TO support CSV export of the currently displayed orders

3.4 WHEN the Products module endpoints are called THEN the system SHALL CONTINUE TO return product data correctly without regression

3.5 WHEN a user is not authenticated and navigates to `/orders` or `/store-orders` THEN the system SHALL CONTINUE TO redirect them via the role guard to the access-denied page

3.6 WHEN the `GET /api/stores/my-store` endpoint is called with a valid Corporate user ID that owns a store THEN the system SHALL CONTINUE TO return the full store entity including `id`, `storeName`, and owner details

---

## Bug Condition Derivation

**Bug Condition Function — Individual Orders:**
```pascal
FUNCTION isBugCondition_IndividualOrders(X)
  INPUT: X of type { userId: Long, pageRequest: Pageable }
  OUTPUT: boolean

  RETURN orderRepository.findByUser(userRepository.findById(X.userId), X.pageRequest).isEmpty()
         AND ordersExistForUser(X.userId)
END FUNCTION
```

**Bug Condition Function — Store Orders:**
```pascal
FUNCTION isBugCondition_StoreOrders(X)
  INPUT: X of type { userId: Long }
  OUTPUT: boolean

  storeResult ← GET /api/stores/my-store?userId=X.userId
  RETURN storeResult.id IS NULL OR storeResult.id IS UNDEFINED
END FUNCTION
```

**Property: Fix Checking — Individual Orders**
```pascal
FOR ALL X WHERE isBugCondition_IndividualOrders(X) DO
  result ← GET /api/orders?userId=X.userId
  ASSERT result.content IS NOT EMPTY
  ASSERT result.totalElements > 0
END FOR
```

**Property: Fix Checking — Store Orders**
```pascal
FOR ALL X WHERE isBugCondition_StoreOrders(X) DO
  storeResult ← GET /api/stores/my-store?userId=X.userId
  ASSERT storeResult.id IS NOT NULL
  ordersResult ← GET /api/store-orders?storeId=storeResult.id
  ASSERT ordersResult.content IS NOT EMPTY
END FOR
```

**Property: Preservation Checking**
```pascal
FOR ALL X WHERE NOT isBugCondition(X) DO
  ASSERT F(X) = F'(X)
  // Pagination, sorting, status updates, CSV export, role guards remain unchanged
END FOR
```
