"""
Configuration module for the Text2SQL chatbot.
Contains database schema definitions and application constants.
"""

import os
from dotenv import load_dotenv

load_dotenv()

# ── API & Service Config ─────────────────────────────────────────────────────
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
SPRING_BASE_URL = os.getenv("SPRING_BASE_URL", "http://localhost:8080")
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

# ── Retry / Safety Constants ─────────────────────────────────────────────────
MAX_ERROR_RETRIES = 3
GEMINI_MAX_RETRIES = 3
GEMINI_INITIAL_BACKOFF_S = 2  # 2s → 4s → 8s

# ── Gemini URL (same pattern as GeminiService.java) ──────────────────────────
GEMINI_URL = (
    f"https://generativelanguage.googleapis.com/v1beta/models/{GEMINI_MODEL}:generateContent"
)

# ── Actual Database Schema (derived from JPA entities) ────────────────────────
DB_SCHEMA = """
-- TABLES AND COLUMNS (PostgreSQL)

-- users: user accounts
--   user_id (BIGINT PK), name (VARCHAR), surname (VARCHAR), email (VARCHAR),
--   password (VARCHAR), phone (VARCHAR), role (VARCHAR: INDIVIDUAL|CORPORATE|ADMIN),
--   created_at (TIMESTAMP)

-- individual_customers: extended profile for individual users
--   user_id (BIGINT PK/FK→users.user_id), gender (VARCHAR: MALE|FEMALE),
--   birth_date (DATE), street (VARCHAR), city (VARCHAR), postal_code (VARCHAR),
--   country (VARCHAR), membership_type (VARCHAR: STANDARD|GOLD|PREMIUM),
--   total_spend (DECIMAL), items_purchased (INT), avg_rating (DOUBLE),
--   discount_applied (BOOLEAN), satisfaction_level (VARCHAR: UNSATISFIED|NEUTRAL|SATISFIED)

-- stores: seller stores
--   store_id (BIGINT PK), store_name (VARCHAR), description (VARCHAR),
--   company_name (VARCHAR), tax_number (VARCHAR), tax_office (VARCHAR),
--   company_address (VARCHAR), total_revenue (DECIMAL), owner_id (BIGINT FK→users.user_id)

-- products: product catalog
--   product_id (BIGINT PK), name (VARCHAR), description (VARCHAR),
--   price (DECIMAL), brand_id (BIGINT FK→brands.id),
--   category_id (BIGINT FK→categories.id), store_id (BIGINT FK→stores.store_id),
--   image_url (VARCHAR), sold_count (INT), created_at (TIMESTAMP)

-- categories: product categories
--   id (BIGINT PK), name (VARCHAR), parent_id (BIGINT nullable self-FK)

-- brands: product brands
--   id (BIGINT PK), name (VARCHAR)

-- orders: customer orders
--   order_id (BIGINT PK), user_id (BIGINT FK→users.user_id),
--   grand_total (DECIMAL), shipping_address (TEXT), created_at (TIMESTAMP)

-- order_items: line items in an order
--   order_item_id (BIGINT PK), order_id (BIGINT FK→orders.order_id),
--   product_id (BIGINT FK→products.product_id), price (DECIMAL), quantity (INT)

-- shipments: shipment tracking
--   shipment_id (BIGINT PK), order_id (BIGINT FK→orders.order_id),
--   shipping_method (VARCHAR: STANDARD|EXPRESS|OVERNIGHT),
--   shipping_cost (DECIMAL), status (VARCHAR: PENDING|SHIPPED|IN_TRANSIT|DELIVERED|CANCELLED),
--   tracking_number (VARCHAR), shipment_date (DATE),
--   estimated_delivery (DATE), delivery_date (DATE)

-- payments: payment records
--   payment_id (BIGINT PK), order_id (BIGINT FK→orders.order_id),
--   payment_method (VARCHAR), payment_status (VARCHAR),
--   amount (DECIMAL), paid_at (TIMESTAMP),
--   payment_provider (VARCHAR: STRIPE|PAYPAL|CRYPTO|COD),
--   card_id (BIGINT FK), currency (VARCHAR), provider_transaction_id (VARCHAR),
--   notes (TEXT)

-- reviews: product reviews
--   review_id (BIGINT PK), user_id (BIGINT FK→users.user_id),
--   product_id (BIGINT FK→products.product_id),
--   rating (INT 1-5), text (VARCHAR 1000), created_at (TIMESTAMP)

-- inventory: product stock levels
--   id (BIGINT PK), store_id (BIGINT), product_id (BIGINT FK→products.product_id),
--   stock (INT)

-- IMPORTANT RELATIONSHIPS:
-- orders.user_id → users.user_id (who placed the order)
-- order_items.order_id → orders.order_id
-- products.store_id → stores.store_id (which store sells it)
-- stores.owner_id → users.user_id (store owner)
-- To find orders for a store: JOIN order_items ON products, then filter by products.store_id
"""

# ── Login endpoint ────────────────────────────────────────────────────────────
LOGIN_URL = f"{SPRING_BASE_URL}/api/auth/login/email"
SQL_EXECUTE_URL = f"{SPRING_BASE_URL}/api/chat/execute"
