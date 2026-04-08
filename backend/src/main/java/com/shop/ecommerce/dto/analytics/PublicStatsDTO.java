package com.shop.ecommerce.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicStatsDTO {
    private long totalUsers;
    private long totalProducts;
    private long totalCategories;
    private long totalBrands;
    private long totalStores;
}
