package com.shop.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for executing SQL queries from the AI chatbot.
 * Contains the raw SQL and user context for audit/validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSqlExecutionDTO {
    private String sql;
    private Long userId;
    private String roleType;
}
