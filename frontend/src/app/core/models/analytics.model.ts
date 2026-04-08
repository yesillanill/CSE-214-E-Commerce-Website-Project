export interface PublicStats {
  totalUsers: number;
  totalProducts: number;
  totalCategories: number;
  totalBrands: number;
  totalStores: number;
}

export interface MonthlyAmount {
  month: string;
  amount: number;
}

export interface CategoryAmount {
  category: string;
  amount: number;
}

export interface RecentOrder {
  orderId: number;
  date: string;
  itemCount: number;
  total: number;
  status: string;
}

export interface IndividualAnalytics {
  totalSpend: number;
  totalOrders: number;
  avgOrderValue: number;
  reviewCount: number;
  monthlySpend: MonthlyAmount[];
  orderStatusDist: Record<string, number>;
  categorySpend: CategoryAmount[];
  recentOrders: RecentOrder[];
}

export interface InventoryStatus {
  productId: number;
  productName: string;
  stock: number;
  lowStock: boolean;
}

export interface TopProduct {
  productName: string;
  salesCount: number;
  revenue: number;
}

export interface CorporateAnalytics {
  totalRevenue: number;
  totalOrders: number;
  activeCustomers: number;
  avgRating: number;
  revenueSeries: MonthlyAmount[];
  categoryRevenue: CategoryAmount[];
  inventoryStatus: InventoryStatus[];
  membershipDist: Record<string, number>;
  topProducts: TopProduct[];
  avgFulfillmentDays: number;
  satisfactionDist: Record<string, number>;
}

export interface StoreComparison {
  storeName: string;
  revenue: number;
  orderCount: number;
}

export interface RegistrationTrend {
  month: string;
  individualCount: number;
  corporateCount: number;
}

export interface AuditLogEntry {
  userEmail: string;
  action: string;
  date: string;
  ipAddress: string;
}

export interface AdminAnalytics {
  platformRevenue: number;
  activeStores: number;
  usersByRole: Record<string, number>;
  avgDailyOrders: number;
  platformAvgRating: number;
  pendingStoreApprovals: number;
  platformRevenueSeries: MonthlyAmount[];
  topStores: StoreComparison[];
  registrationTrend: RegistrationTrend[];
  categoryPerformance: CategoryAmount[];
  recentAuditLogs: AuditLogEntry[];
}
