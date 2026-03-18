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
            
            // Sync PostgreSQL sequences (fixes duplicate key value violates unique constraint when inserting into carts/wishlists)
            jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('carts', 'cart_id'), coalesce(max(cart_id), 1), max(cart_id) IS NOT NULL) FROM carts");
            jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('wishlists', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM wishlists");
            
            logger.info("Successfully sanitized Enum constraints and synchronized database sequences.");
        } catch (Exception e) {
            logger.error("Failed to execute database fix script: " + e.getMessage());
        }
    }
}
