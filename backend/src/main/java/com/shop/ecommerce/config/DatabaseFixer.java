package com.shop.ecommerce.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DatabaseFixer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseFixer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Fix existing Enum strings in the DB before Hibernate crashes querying IndividualCustomer
            jdbcTemplate.execute("UPDATE individual_customers SET gender = UPPER(gender) WHERE gender IS NOT NULL");
            jdbcTemplate.execute("UPDATE individual_customers SET membership_type = UPPER(membership_type) WHERE membership_type IS NOT NULL");
            jdbcTemplate.execute("UPDATE individual_customers SET satisfaction_level = UPPER(satisfaction_level) WHERE satisfaction_level IS NOT NULL");
            
            // Sync PostgreSQL sequences (fixes duplicate key value violates unique constraint when inserting)
            jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('carts', 'cart_id'), coalesce(max(cart_id), 1), max(cart_id) IS NOT NULL) FROM carts");
            jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('wishlists', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM wishlists");
            jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('shipments', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM shipments");
            jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('payments', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM payments");

            // Fill null created_at with shipment_date or a default date
            jdbcTemplate.execute("""
                UPDATE orders SET created_at = COALESCE(
                    (SELECT s.shipment_date::timestamp FROM shipments s WHERE s.order_id = orders.order_id),
                    NOW() - INTERVAL '30 days' * RANDOM()
                ) WHERE created_at IS NULL
            """);

            // Fill null shipping_address for orders that have none
            jdbcTemplate.execute("""
                UPDATE orders SET shipping_address = sub.full_address
                FROM (
                    SELECT o.order_id,
                           CONCAT(
                               COALESCE(ic.street, ''),
                               CASE WHEN ic.street IS NOT NULL THEN ', ' ELSE '' END,
                               COALESCE(ic.city, ''),
                               CASE WHEN ic.postal_code IS NOT NULL THEN ' ' ELSE '' END,
                               COALESCE(ic.postal_code, ''),
                               CASE WHEN ic.country IS NOT NULL THEN ', ' ELSE '' END,
                               COALESCE(ic.country, '')
                           ) AS full_address
                    FROM orders o
                    JOIN individual_customers ic ON ic.user_id = o.user_id
                    WHERE o.shipping_address IS NULL
                       OR o.shipping_address LIKE '%address not provided%'
                ) sub
                WHERE orders.order_id = sub.order_id
                  AND sub.full_address IS NOT NULL
                  AND sub.full_address <> ''
            """);

            // Fill null payment records — create PENDING payments for orders without one
            jdbcTemplate.execute("""
                INSERT INTO payments (order_id, payment_method, payment_status, amount)
                SELECT o.order_id, 'CREDIT_CARD', 'PENDING', o.grand_total
                FROM orders o
                WHERE NOT EXISTS (SELECT 1 FROM payments p WHERE p.order_id = o.order_id)
            """);

            // Fill null shipment records — create PROCESSING shipments for orders without one
            jdbcTemplate.execute("""
                INSERT INTO shipments (order_id, status, shipping_method, shipping_cost, estimated_delivery)
                SELECT o.order_id, 'PROCESSING', 'STANDARD', 0,
                       COALESCE(o.created_at::date, CURRENT_DATE) + INTERVAL '7 days'
                FROM orders o
                WHERE NOT EXISTS (SELECT 1 FROM shipments s WHERE s.order_id = o.order_id)
            """);

            // Normalize payment_method values to match PaymentMethod enum
            jdbcTemplate.execute("UPDATE payments SET payment_method = 'CASH_ON_DELIVERY' WHERE payment_method IN ('cod', 'cashatdoorstep')");
            jdbcTemplate.execute("UPDATE payments SET payment_method = 'CREDIT_CARD' WHERE payment_method IN ('ublcreditcard', 'customercredit', 'productcredit')");
            jdbcTemplate.execute("UPDATE payments SET payment_method = 'BANK_TRANSFER' WHERE payment_method IN ('internetbanking')");
            jdbcTemplate.execute("UPDATE payments SET payment_method = 'DIGITAL_WALLET' WHERE payment_method IN ('mygateway', 'marketingexpense')");
            jdbcTemplate.execute("UPDATE payments SET payment_method = 'DEBIT_CARD' WHERE payment_method IN ('mcblite')");
            jdbcTemplate.execute("""
                UPDATE payments SET payment_method = 'CREDIT_CARD'
                WHERE payment_method NOT IN ('CREDIT_CARD','DEBIT_CARD','BANK_TRANSFER','CASH_ON_DELIVERY','DIGITAL_WALLET')
                   OR payment_method IS NULL
            """);

            // Normalize payment_status values
            jdbcTemplate.execute("UPDATE payments SET payment_status = 'COMPLETED' WHERE LOWER(payment_status) = 'paid'");
            jdbcTemplate.execute("UPDATE payments SET payment_status = 'PENDING' WHERE payment_status IS NULL");

            // Fix existing shipment records that have null fields
            jdbcTemplate.execute("UPDATE shipments SET status = 'PROCESSING' WHERE status IS NULL");
            jdbcTemplate.execute("UPDATE shipments SET shipping_method = 'STANDARD' WHERE shipping_method IS NULL");
            jdbcTemplate.execute("UPDATE shipments SET shipping_cost = 0 WHERE shipping_cost IS NULL");
            jdbcTemplate.execute("""
                UPDATE shipments SET tracking_number = CONCAT('TRK', LPAD(FLOOR(RANDOM()*999999999)::TEXT, 9, '0'))
                WHERE tracking_number IS NULL
            """);
            jdbcTemplate.execute("""
                UPDATE shipments SET estimated_delivery = COALESCE(shipment_date + INTERVAL '7 days', CURRENT_DATE + INTERVAL '7 days')
                WHERE estimated_delivery IS NULL
            """);

            logger.info("Successfully sanitized Enum constraints, synchronized sequences, and fixed null order data.");
        } catch (Exception e) {
            logger.error("Failed to execute database fix script: " + e.getMessage());
        }
    }
}