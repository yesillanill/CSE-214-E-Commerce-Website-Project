import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import Swal from 'sweetalert2';

interface OrderItem { id: number; productName: string; productId: number; brandName: string; storeName: string; unitPrice: number; quantity: number; lineTotal: number; }
interface StoreOrder { id: number; createdAt: string; grandTotal: number; paymentStatus: string; shippingAddress: string; shipmentStatus: string; orderItems: OrderItem[]; }

@Component({
  selector: 'app-store-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, DecimalPipe, DatePipe],
  templateUrl: './store-orders.html',
  styleUrl: './store-orders.scss',
})
export class StoreOrders implements OnInit {
  orders: StoreOrder[] = [];
  expandedOrderId: number | null = null;
  isLoading = true;
  storeId: number | null = null;
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  pageSizeOptions = [10, 25, 50];
  searchTerm = '';
  sortBy = 'createdAt';
  sortDir = 'desc';

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit() {
    const user = this.auth.getUser();
    if (user) {
      this.http.get<any>(`http://localhost:8080/api/stores/my-store?userId=${user.id}`).subscribe({
        next: (store: any) => {
          if (store && store.id) {
            this.storeId = store.id;
            this.loadOrders();
          } else { this.isLoading = false; }
        },
        error: () => { this.isLoading = false; }
      });
    }
  }

  loadOrders() {
    if (!this.storeId) return;
    this.isLoading = true;
    let url = `http://localhost:8080/api/store-orders?storeId=${this.storeId}&page=${this.currentPage}&size=${this.pageSize}&sortBy=${this.sortBy}&sortDir=${this.sortDir}`;
    this.http.get<any>(url).subscribe({
      next: (res) => {
        this.orders = res.content || [];
        this.totalPages = res.totalPages || 0;
        this.totalElements = res.totalElements || 0;
        this.isLoading = false;
      },
      error: () => { this.orders = []; this.isLoading = false; }
    });
  }

  toggleExpand(orderId: number) { this.expandedOrderId = this.expandedOrderId === orderId ? null : orderId; }

  sort(column: string) {
    if (this.sortBy === column) { this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc'; }
    else { this.sortBy = column; this.sortDir = 'desc'; }
    this.currentPage = 0;
    this.loadOrders();
  }

  getSortIcon(col: string): string {
    if (this.sortBy !== col) return '↕';
    return this.sortDir === 'asc' ? '↑' : '↓';
  }

  updateStatus(orderId: number, status: string) {
    if (status === 'REJECTED') {
      Swal.fire({ title: 'Reject Order', text: 'Are you sure?', icon: 'warning', showCancelButton: true, confirmButtonText: 'Reject', confirmButtonColor: '#ff4d4d' })
        .then((r) => { if (r.isConfirmed) this.sendStatusUpdate(orderId, status); });
    } else { this.sendStatusUpdate(orderId, status); }
  }

  private sendStatusUpdate(orderId: number, status: string) {
    this.http.patch<any>(`http://localhost:8080/api/store-orders/${orderId}/status`, { status }).subscribe({
      next: () => { const o = this.orders.find(x => x.id === orderId); if (o) o.shipmentStatus = status; Swal.fire('Updated!', `Status → ${status}`, 'success'); },
      error: () => Swal.fire('Error', 'Could not update.', 'error')
    });
  }

  exportCsv() {
    const header = 'Order ID,Date,Total,Payment Status,Shipment Status,Address\n';
    const rows = this.orders.map(o =>
      `${o.id},${o.createdAt},${o.grandTotal},${o.paymentStatus || ''},${o.shipmentStatus || ''},"${o.shippingAddress || ''}"`
    ).join('\n');
    const blob = new Blob([header + rows], { type: 'text/csv' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'store_orders.csv';
    a.click();
  }

  getStatusClass(s: string): string {
    switch (s) {
      case 'DELIVERED': case 'COMPLETED': return 'status-success';
      case 'SHIPPED': case 'IN_TRANSIT': return 'status-info';
      case 'PROCESSING': case 'PENDING': return 'status-warning';
      case 'REJECTED': case 'FAILED': return 'status-danger';
      default: return '';
    }
  }

  canShip(s: string): boolean { return s === 'PROCESSING' || s === 'PENDING'; }
  canReject(s: string): boolean { return s !== 'REJECTED' && s !== 'DELIVERED' && s !== 'SHIPPED'; }
  goToPage(p: number) { if (p >= 0 && p < this.totalPages) { this.currentPage = p; this.loadOrders(); } }
  onPageSizeChange(e: Event) { this.pageSize = +(e.target as HTMLSelectElement).value; this.currentPage = 0; this.loadOrders(); }
}
