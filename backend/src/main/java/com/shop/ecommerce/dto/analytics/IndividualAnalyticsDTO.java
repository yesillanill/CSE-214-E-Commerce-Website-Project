package com.shop.ecommerce.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndividualAnalyticsDTO {
    private double totalSpend;
    private long totalOrders;
    private double avgOrderValue;
    private long reviewCount;
    private List<MonthlyAmountDTO> monthlySpend;
    private Map<String, Long> orderStatusDist;
    private List<CategoryAmountDTO> categorySpend;
    private List<RecentOrderDTO> recentOrders;
}
