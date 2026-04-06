package com.shop.ecommerce.controller;

import com.shop.ecommerce.entities.AuditLog;
import com.shop.ecommerce.enums.AuditAction;
import com.shop.ecommerce.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<Page<AuditLog>> getLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        AuditAction auditAction = null;
        if (action != null && !action.isEmpty()) {
            try { auditAction = AuditAction.valueOf(action); } catch (Exception ignored) {}
        }

        LocalDateTime start = startDate != null && !startDate.isEmpty() ? LocalDateTime.parse(startDate + "T00:00:00") : null;
        LocalDateTime end = endDate != null && !endDate.isEmpty() ? LocalDateTime.parse(endDate + "T23:59:59") : null;
        String roleFilter = (userRole != null && !userRole.isEmpty()) ? userRole : null;
        String emailFilter = (email != null && !email.isEmpty()) ? "%" + email.toLowerCase() + "%" : null;

        return ResponseEntity.ok(auditLogRepository.findFiltered(auditAction, roleFilter, emailFilter, start, end, pageable));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        AuditAction auditAction = null;
        if (action != null && !action.isEmpty()) {
            try { auditAction = AuditAction.valueOf(action); } catch (Exception ignored) {}
        }
        LocalDateTime start = startDate != null && !startDate.isEmpty() ? LocalDateTime.parse(startDate + "T00:00:00") : null;
        LocalDateTime end = endDate != null && !endDate.isEmpty() ? LocalDateTime.parse(endDate + "T23:59:59") : null;
        String roleFilter = (userRole != null && !userRole.isEmpty()) ? userRole : null;
        String emailFilter = (email != null && !email.isEmpty()) ? "%" + email.toLowerCase() + "%" : null;

        Pageable pageable = PageRequest.of(0, 10000, Sort.by("createdAt").descending());
        List<AuditLog> logs = auditLogRepository.findFiltered(auditAction, roleFilter, emailFilter, start, end, pageable).getContent();

        StringBuilder csv = new StringBuilder("Log ID,Timestamp,User Email,User Role,Action,Target Entity,IP Address,Details\n");
        for (AuditLog log : logs) {
            csv.append(String.format("%d,%s,%s,%s,%s,%s,%s,\"%s\"\n",
                    log.getId(),
                    log.getCreatedAt(),
                    log.getUserEmail() != null ? log.getUserEmail() : "",
                    log.getUserRole() != null ? log.getUserRole() : "",
                    log.getAction() != null ? log.getAction().name() : "",
                    log.getTargetEntity() != null ? log.getTargetEntity() : "",
                    log.getIpAddress() != null ? log.getIpAddress() : "",
                    log.getDetails() != null ? log.getDetails().replace("\"", "'") : ""
            ));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "audit_logs.csv");

        return ResponseEntity.ok().headers(headers).body(csv.toString().getBytes());
    }
}
