package com.shop.ecommerce.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsDTO {
    private double platformRevenue;
    private long activeStores;
    private Map<String, Long> usersByRole;
    private double avgDailyOrders;
    private double platformAvgRating;
    private long pendingStoreApprovals;
    private List<MonthlyAmountDTO> platformRevenueSeries;
    private List<StoreComparisonDTO> topStores;
    private List<RegistrationTrendDTO> registrationTrend;
    private List<CategoryAmountDTO> categoryPerformance;
    private List<AuditLogEntryDTO> recentAuditLogs;
}
