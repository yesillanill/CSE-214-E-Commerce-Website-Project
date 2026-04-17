-- =====================================================
-- Docker PostgreSQL Initialization Script
-- This runs ONLY on first container startup (fresh volume)
-- Spring Boot's ddl-auto=update handles table creation
-- =====================================================

-- Ensure the ecommerce database exists (created by POSTGRES_DB env var)
-- This script is for any additional initialization if needed

-- Grant all privileges to postgres user
GRANT ALL PRIVILEGES ON DATABASE ecommerce TO postgres;
