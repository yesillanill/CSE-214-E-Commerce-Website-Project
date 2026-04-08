package com.shop.ecommerce.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntryDTO {
    private String userEmail;
    private String action;
    private LocalDateTime date;
    private String ipAddress;
}
