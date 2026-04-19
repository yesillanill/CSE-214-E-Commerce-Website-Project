package com.shop.ecommerce.config;

import com.shop.ecommerce.config.SqlInjectionFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for validating text inputs against SQL injection patterns.
 * Used by service classes to validate user-submitted text fields
 * (reviews, support tickets, product descriptions, etc.)
 */
@Slf4j
@Component
public class SqlInjectionValidator {

    /**
     * Patterns that should NEVER appear in user-generated text content.
     * More aggressive than the filter — designed for text fields like reviews and messages.
     */
    private static final Pattern TEXT_SQL_INJECTION = Pattern.compile(
            // Classic injection payloads
            "('\\s*(OR|AND)\\s+\\d+\\s*=\\s*\\d+)" +
            // Statement terminators + keywords
            "|(;\\s*(DROP|ALTER|CREATE|TRUNCATE|INSERT|UPDATE|DELETE|EXEC|SELECT)\\b)" +
            // UNION SELECT
            "|(\\bUNION\\s+(ALL\\s+)?SELECT\\b)" +
            // SQL comments
            "|(--\\s)" +
            "|(/\\*)" +
            // Hex encoding
            "|(0x[0-9a-fA-F]{8,})" +
            // Function-based attacks
            "|(\\b(CHAR|CONCAT|SUBSTRING|ASCII|CAST|CONVERT)\\s*\\()" +
            // Time-based blind injection
            "|(\\b(WAITFOR|SLEEP|BENCHMARK|PG_SLEEP)\\s*\\()" +
            // Schema probing
            "|(\\bINFORMATION_SCHEMA\\b)" +
            "|(\\b(PG_CATALOG|PG_TABLES|SYS\\.OBJECTS)\\b)" +
            // Dangerous standalone statements
            "|(\\bDROP\\s+TABLE\\b)" +
            "|(\\bALTER\\s+TABLE\\b)" +
            "|(\\bTRUNCATE\\s+TABLE\\b)" +
            "|(\\bDELETE\\s+FROM\\b)" +
            "|(\\bINSERT\\s+INTO\\b)" +
            "|(\\bUPDATE\\s+\\w+\\s+SET\\b)" +
            "|(\\bCREATE\\s+TABLE\\b)" +
            // xp_ procedures
            "|(\\bxp_\\w+)" +
            // File operations
            "|(\\b(LOAD_FILE|INTO\\s+OUTFILE|INTO\\s+DUMPFILE)\\b)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Validate a text field for SQL injection.
     *
     * @param fieldName Name of the field (for logging).
     * @param value     The text value to check.
     * @throws IllegalArgumentException if SQL injection is detected.
     */
    public void validate(String fieldName, String value) {
        if (value == null || value.isBlank()) return;

        if (TEXT_SQL_INJECTION.matcher(value).find()) {
            log.warn("SQL injection detected in field '{}': {}", fieldName,
                    value.length() > 100 ? value.substring(0, 100) + "..." : value);
            throw new IllegalArgumentException(
                    "Input contains potentially dangerous SQL patterns. Please remove SQL commands and try again.");
        }
    }

    /**
     * Check if a string contains SQL injection (without throwing).
     */
    public boolean containsSqlInjection(String value) {
        if (value == null || value.isBlank()) return false;
        return TEXT_SQL_INJECTION.matcher(value).find();
    }
}
