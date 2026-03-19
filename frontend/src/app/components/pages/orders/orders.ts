import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';

interface OrderItem {
  id: number;
  productName: string;
  productId: number;
  brandName: string;
  brandId: number;
  storeName: string;
  storeId: number;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

interface Order {
  id: number;
  createdAt: string;
  grandTotal: number;
  paymentStatus: string;
  paymentMethod: string;
  shippingMethod: string;
  shippingCost: number;
  shipmentDate: string;
  shipmentStatus: string;
  deliveryDate: string;
  estimatedDelivery: string;
  trackingNumber: string;
  orderItems: OrderItem[];
}

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule, DecimalPipe, DatePipe],
  templateUrl: './orders.html',
  styleUrl: './orders.scss',
})
export class Orders implements OnInit {
  orders: Order[] = [];
  expandedOrderId: number | null = null;
  isLoading = true;

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  pageSizeOptions = [10, 25, 50];

  // Sorting
  sortBy = 'createdAt';
  sortDir = 'desc';

  constructor(
    private http: HttpClient,
    private auth: AuthService
  ) {}

  ngOnInit() {
    this.loadOrders();
  }

  loadOrders() {
    this.isLoading = true;
    const user = this.auth.getUser();
    if (!user) return;

    const url = `http://localhost:8080/api/orders?userId=${user.id}&page=${this.currentPage}&size=${this.pageSize}&sortBy=${this.sortBy}&sortDir=${this.sortDir}`;
    this.http.get<any>(url).subscribe({
      next: (res) => {
        this.orders = res.content || [];
        this.totalPages = res.totalPages || 0;
        this.totalElements = res.totalElements || 0;
        this.isLoading = false;
      },
      error: () => {
        this.orders = [];
        this.isLoading = false;
      }
    });
  }

  toggleExpand(orderId: number) {
    this.expandedOrderId = this.expandedOrderId === orderId ? null : orderId;
  }

  sort(column: string) {
    if (this.sortBy === column) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = column;
      this.sortDir = 'desc';
    }
    this.currentPage = 0;
    this.loadOrders();
  }

  getSortIcon(column: string): string {
    if (this.sortBy !== column) return '↕';
    return this.sortDir === 'asc' ? '↑' : '↓';
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadOrders();
    }
  }

  onPageSizeChange(event: Event) {
    this.pageSize = +(event.target as HTMLSelectElement).value;
    this.currentPage = 0;
    this.loadOrders();
  }

  getDeliveryDisplay(order: Order): string {
    if (order.shipmentStatus === 'DELIVERED' && order.deliveryDate) {
      return order.deliveryDate;
    }
    if (order.estimatedDelivery) {
      return 'Est. ' + order.estimatedDelivery;
    }
    return '-';
  }

  getStatusClass(status: string): string {
    if (!status) return '';
    switch (status) {
      case 'DELIVERED':
      case 'COMPLETED': return 'status-success';
      case 'SHIPPED':
      case 'IN_TRANSIT': return 'status-info';
      case 'PROCESSING':
      case 'PENDING': return 'status-warning';
      case 'REJECTED':
      case 'FAILED': return 'status-danger';
      default: return '';
    }
  }
}
