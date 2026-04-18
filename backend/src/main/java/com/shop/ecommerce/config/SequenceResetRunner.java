package com.shop.ecommerce.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Resets PostgreSQL sequences on application startup so they are in sync
 * with the actual maximum IDs.  This prevents "duplicate key" errors when
 * the database was seeded with explicit ID values.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SequenceResetRunner implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        resetSequence("reviews", "review_id");
        resetSequence("orders", "order_id");
        resetSequence("payments", "payment_id");
        resetSequence("shipments", "shipment_id");
    }

    private void resetSequence(String table, String column) {
        try {
            // Find the sequence name used by the IDENTITY / SERIAL column
            String seqName = jdbc.queryForObject(
                    "SELECT pg_get_serial_sequence(?, ?)",
                    String.class, table, column);

            if (seqName == null) {
                log.debug("No sequence found for {}.{} — skipping.", table, column);
                return;
            }

            // Reset the sequence to MAX(column) + 1  (or 1 if table is empty)
            Long maxId = jdbc.queryForObject(
                    "SELECT COALESCE(MAX(" + column + "), 0) FROM " + table,
                    Long.class);

            jdbc.execute("SELECT setval('" + seqName + "', " + (maxId + 1) + ", false)");
            log.info("Reset sequence {} to {} for {}.{}", seqName, maxId + 1, table, column);
        } catch (Exception e) {
            log.warn("Could not reset sequence for {}.{}: {}", table, column, e.getMessage());
        }
    }
}
