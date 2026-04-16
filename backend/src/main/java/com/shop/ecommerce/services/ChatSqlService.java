package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.ChatSqlExecutionDTO;
import com.shop.ecommerce.dto.ChatSqlResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for executing read-only SQL queries from the AI chatbot.
 * Validates that only SELECT statements are accepted and limits result sets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSqlService {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static final int MAX_ROWS = 1000;

    /**
     * Dangerous SQL pattern — any statement that is not a SELECT.
     */
    private static final Pattern DANGEROUS_SQL = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TRUNCATE|EXEC|EXECUTE|GRANT|REVOKE|MERGE|CALL)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Prohibited syntax (multi-statement or exfiltration)
     */
    private static final Pattern PROHIBITED_SYNTAX = Pattern.compile(
            "(;|--|/\\*|\\*\\/|xp_)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Select * detection
     */
    private static final Pattern SELECT_STAR = Pattern.compile(
            "\\bSELECT\\s+\\*\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Sensitive columns detection
     */
    private static final Pattern SENSITIVE_COLUMNS = Pattern.compile(
            "\\b(password|password_hash|api_key|secret_key|oauth_token|private_key|session_token|credit_card|cvv|ssn)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Must start with SELECT or WITH (for CTEs).
     */
    private static final Pattern VALID_START = Pattern.compile(
            "^\\s*(SELECT|WITH)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Execute a read-only SQL query and return the results.
     *
     * @param dto The execution request containing SQL and user context.
     * @return Result DTO with the query results or an error message.
     */
    public ChatSqlResultDTO executeQuery(ChatSqlExecutionDTO dto) {
        String sql = dto.getSql();

        // ── Validation ───────────────────────────────────────────────────
        if (sql == null || sql.isBlank()) {
            return ChatSqlResultDTO.builder()
                    .error("SQL query is empty.")
                    .results(List.of())
                    .columns(List.of())
                    .rowCount(0)
                    .build();
        }

        sql = sql.trim();

        if (!VALID_START.matcher(sql).find()) {
            log.warn("ChatSqlService — rejected non-SELECT query from userId={}", dto.getUserId());
            return ChatSqlResultDTO.builder()
                    .error("Only SELECT queries are allowed.")
                    .results(List.of())
                    .columns(List.of())
                    .rowCount(0)
                    .build();
        }

        if (DANGEROUS_SQL.matcher(sql).find()) {
            log.warn("ChatSqlService — rejected dangerous SQL from userId={}", dto.getUserId());
            return ChatSqlResultDTO.builder()
                    .error("Query contains disallowed statements.")
                    .results(List.of())
                    .columns(List.of())
                    .rowCount(0)
                    .build();
        }

        if (PROHIBITED_SYNTAX.matcher(sql).find()) {
            log.warn("ChatSqlService — rejected prohibited syntax from userId={}", dto.getUserId());
            return ChatSqlResultDTO.builder()
                    .error("Query contains prohibited characters like multi-statement terminators or comments.")
                    .results(List.of())
                    .columns(List.of())
                    .rowCount(0)
                    .build();
        }

        if (SELECT_STAR.matcher(sql).find()) {
            log.warn("ChatSqlService — rejected SELECT * from userId={}", dto.getUserId());
            return ChatSqlResultDTO.builder()
                    .error("SELECT * is not allowed. Please specify columns explicitly.")
                    .results(List.of())
                    .columns(List.of())
                    .rowCount(0)
                    .build();
        }

        if (SENSITIVE_COLUMNS.matcher(sql).find()) {
            log.warn("ChatSqlService — rejected sensitive column query from userId={}", dto.getUserId());
            return ChatSqlResultDTO.builder()
                    .error("Query requests prohibited sensitive columns.")
                    .results(List.of())
                    .columns(List.of())
                    .rowCount(0)
                    .build();
        }

        // ── Execution ────────────────────────────────────────────────────
        try {
            // Add LIMIT if not already present to prevent huge result sets
            String execSql = sql;
            if (!sql.toUpperCase().contains("LIMIT")) {
                execSql = sql + " LIMIT " + MAX_ROWS;
            }

            log.info("ChatSqlService — executing parameterized query for userId={}, corpId={}, roleType={}",
                    dto.getUserId(), dto.getCorpId(), dto.getRoleType());

            Map<String, Object> params = Map.of(
                    "userId", dto.getUserId() != null ? dto.getUserId() : 0L,
                    "corpId", dto.getCorpId() != null ? dto.getCorpId() : 0L
            );

            List<Map<String, Object>> results = namedParameterJdbcTemplate.queryForList(execSql, params);

            // Extract column names from first row (if any)
            List<String> columns = new ArrayList<>();
            if (!results.isEmpty()) {
                columns.addAll(results.get(0).keySet());
            }

            log.info("ChatSqlService — query returned {} rows", results.size());

            return ChatSqlResultDTO.builder()
                    .results(results)
                    .columns(columns)
                    .rowCount(results.size())
                    .build();

        } catch (Exception e) {
            log.error("ChatSqlService — query execution failed for userId={}: {}",
                    dto.getUserId(), e.getMessage());

            String safeError = e.getMessage();
            if (safeError != null && safeError.length() > 500) {
                safeError = safeError.substring(0, 500);
            }

            return ChatSqlResultDTO.builder()
                    .error(safeError)
                    .results(List.of())
                    .columns(List.of())
                    .rowCount(0)
                    .build();
        }
    }
}
