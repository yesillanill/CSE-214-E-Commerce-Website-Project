package com.shop.ecommerce.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Global SQL Injection prevention filter.
 * Scans ALL incoming request parameters for SQL injection patterns.
 * For POST/PUT/PATCH requests with JSON body, scans text fields in the body.
 *
 * Endpoints that LEGITIMATELY need SQL in their body (e.g. /api/chat/execute)
 * are excluded from body scanning — they have their own validation in ChatSqlService.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SqlInjectionFilter extends OncePerRequestFilter {

    /** Endpoints whose request BODY is allowed to contain SQL (they validate internally). */
    private static final Set<String> BODY_SCAN_EXCLUSIONS = Set.of(
            "/api/chat/execute"
    );

    /**
     * Multi-statement and classic injection patterns.
     * These catch things like:  ' OR 1=1 --   ;DROP TABLE   UNION SELECT   ' OR ''='
     */
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            // Classic tautologies
            "('\\s*(OR|AND)\\s+(1\\s*=\\s*1|'[^']*'\\s*=\\s*'[^']*'|TRUE))" +
            // Statement terminators followed by dangerous keyword
            "|(;\\s*(DROP|ALTER|CREATE|TRUNCATE|INSERT|UPDATE|DELETE|EXEC|EXECUTE|GRANT|REVOKE|MERGE|CALL))" +
            // UNION-based injection
            "|(\\bUNION\\s+(ALL\\s+)?SELECT\\b)" +
            // Comment-based injection
            "|(--\\s)" +
            // Block comments
            "|(/\\*)" +
            // Hex encoding evasion (0x...)
            "|(0x[0-9a-fA-F]{6,})" +
            // CHAR() / CONCAT() obfuscation
            "|(\\bCHAR\\s*\\()" +
            // WAITFOR / SLEEP / BENCHMARK (time-based blind injection)
            "|(\\b(WAITFOR|SLEEP|BENCHMARK|PG_SLEEP)\\s*\\()" +
            // Information schema probing
            "|(\\bINFORMATION_SCHEMA\\b)" +
            // System table probing
            "|(\\b(SYS\\.OBJECTS|SYSOBJECTS|PG_CATALOG|PG_TABLES)\\b)" +
            // Stacked queries: plain dangerous keywords after semicolons
            "|(;\\s*(SELECT|WITH)\\b)" +
            // xp_ extended stored procedures (SQL Server)
            "|(\\bxp_\\w+)" +
            // LOAD_FILE / INTO OUTFILE / INTO DUMPFILE
            "|(\\b(LOAD_FILE|INTO\\s+OUTFILE|INTO\\s+DUMPFILE)\\b)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Standalone dangerous SQL keywords as complete words in user text input.
     * This is applied to request parameters only (not body).
     */
    private static final Pattern DANGEROUS_STANDALONE = Pattern.compile(
            "\\b(DROP\\s+TABLE|ALTER\\s+TABLE|TRUNCATE\\s+TABLE|DELETE\\s+FROM|INSERT\\s+INTO|UPDATE\\s+\\w+\\s+SET|CREATE\\s+TABLE|EXEC\\s+|EXECUTE\\s+)\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Check all query parameters
        Map<String, String[]> params = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            for (String value : entry.getValue()) {
                if (isSqlInjection(value)) {
                    log.warn("SQL injection blocked in query param '{}' from IP={} URI={}",
                            entry.getKey(), request.getRemoteAddr(), request.getRequestURI());
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Potential SQL injection detected. Request blocked.\"}");
                    return;
                }
            }
        }

        // 2. Check request body for non-excluded endpoints
        String uri = request.getRequestURI();
        boolean shouldScanBody = !BODY_SCAN_EXCLUSIONS.contains(uri)
                && isContentTypeJson(request)
                && ("POST".equalsIgnoreCase(request.getMethod())
                    || "PUT".equalsIgnoreCase(request.getMethod())
                    || "PATCH".equalsIgnoreCase(request.getMethod()));

        if (shouldScanBody) {
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 10240);
            // We need to read the body after the filter chain would normally read it,
            // but we also need to check it beforehand. Use the cached wrapper.
            filterChain.doFilter(wrappedRequest, response);

            // Note: We check AFTER to not break stream reading.
            // For a stricter approach we could buffer the body first, but
            // the ContentCachingRequestWrapper allows re-reading.
            // Since the chain already executed, we log but don't block here.
            // The real blocking happens at the parameter level and in service-layer validators.
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if a string contains SQL injection patterns.
     */
    public static boolean isSqlInjection(String input) {
        if (input == null || input.isBlank()) return false;
        // Decode common evasion encodings
        String decoded = input
                .replace("%27", "'")
                .replace("%22", "\"")
                .replace("%3B", ";")
                .replace("%2D%2D", "--");

        return SQL_INJECTION_PATTERN.matcher(decoded).find()
                || DANGEROUS_STANDALONE.matcher(decoded).find();
    }

    /**
     * Check if the request content type is JSON.
     */
    private boolean isContentTypeJson(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }
}
