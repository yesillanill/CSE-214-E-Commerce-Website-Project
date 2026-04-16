package com.shop.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for SQL query execution results from the AI chatbot.
 * Returns the result set as a list of column→value maps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSqlResultDTO {
    private List<Map<String, Object>> results;
    private List<String> columns;
    private int rowCount;
    private String error;
}
