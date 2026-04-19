package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.AuditLog;
import com.shop.ecommerce.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(CAST(:action AS text) IS NULL OR a.action = :action) AND " +
           "(CAST(:userRole AS text) IS NULL OR a.userRole = :userRole) AND " +
           "(CAST(:email AS text) IS NULL OR (LOWER(a.userEmail) LIKE LOWER(CAST(:email AS text)) OR LOWER(a.targetEntity) LIKE LOWER(CAST(:email AS text)))) AND " +
           "(CAST(:startDate AS timestamp) IS NULL OR a.createdAt >= :startDate) AND " +
           "(CAST(:endDate AS timestamp) IS NULL OR a.createdAt <= :endDate)")
    Page<AuditLog> findFiltered(
            @Param("action") AuditAction action,
            @Param("userRole") String userRole,
            @Param("email") String email,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
