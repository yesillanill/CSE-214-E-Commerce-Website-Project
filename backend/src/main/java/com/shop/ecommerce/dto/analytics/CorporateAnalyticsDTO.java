package com.shop.ecommerce.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorporateAnalyticsDTO {
    private double totalRevenue;
    private long totalOrders;
    private long activeCustomers;
    private double avgRating;
    private List<MonthlyAmountDTO> revenueSeries;
    private List<CategoryAmountDTO> categoryRevenue;
    private List<InventoryStatusDTO> inventoryStatus;
    private Map<String, Long> membershipDist;
    private List<TopProductDTO> topProducts;
    private double avgFulfillmentDays;
    private Map<String, Long> satisfactionDist;
}
