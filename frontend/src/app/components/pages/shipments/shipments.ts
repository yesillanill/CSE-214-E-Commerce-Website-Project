import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-shipments',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, DecimalPipe, DatePipe],
  templateUrl: './shipments.html',
  styleUrl: './shipments.scss',
})
export class Shipments implements OnInit {
  shipments: any[] = [];
  isLoading = true;
  storeId: number | null = null;
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  pageSizeOptions = [10, 25, 50];
  searchTerm = '';
  sortBy = 'shipmentDate';
  sortDir = 'desc';

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit() {
    const user = this.auth.getUser();
    if (user) {
      this.http.get<any>(`http://localhost:8080/api/stores/my-store?userId=${user.id}`).subscribe({
        next: (store: any) => {
          if (store && store.id) { this.storeId = store.id; this.loadShipments(); }
          else { this.isLoading = false; }
        },
        error: () => { this.isLoading = false; }
      });
    }
  }

  loadShipments() {
    if (!this.storeId) return;
    this.isLoading = true;
    let url = `http://localhost:8080/api/shipments?storeId=${this.storeId}&page=${this.currentPage}&size=${this.pageSize}&sortBy=${this.sortBy}&sortDir=${this.sortDir}`;
    if (this.searchTerm) url += `&search=${this.searchTerm}`;

    this.http.get<any>(url).subscribe({
      next: (res) => {
        this.shipments = res.content || [];
        this.totalPages = res.totalPages || 0;
        this.totalElements = res.totalElements || 0;
        this.isLoading = false;
      },
      error: () => { this.shipments = []; this.isLoading = false; }
    });
  }

  onSearch(event: Event) {
    this.searchTerm = (event.target as HTMLInputElement).value;
    this.currentPage = 0;
    this.loadShipments();
  }

  sort(column: string) {
    if (this.sortBy === column) { this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc'; }
    else { this.sortBy = column; this.sortDir = 'desc'; }
    this.currentPage = 0;
    this.loadShipments();
  }

  getSortIcon(col: string): string {
    if (this.sortBy !== col) return '↕';
    return this.sortDir === 'asc' ? '↑' : '↓';
  }

  getStatusClass(s: string): string {
    switch (s) {
      case 'DELIVERED': return 'status-success';
      case 'SHIPPED': case 'IN_TRANSIT': return 'status-info';
      case 'PROCESSING': case 'PENDING': return 'status-warning';
      case 'RETURNED': case 'FAILED': return 'status-danger';
      default: return '';
    }
  }

  exportCsv() {
    const header = 'Shipment ID,Order ID,Tracking,Status,Ship Date,Est. Delivery,Delivery Date,Carrier\n';
    const rows = this.shipments.map(s =>
      `${s.id},${s.orderId},${s.trackingNumber || ''},${s.status || ''},${s.shipmentDate || ''},${s.estimatedDelivery || ''},${s.deliveryDate || ''},${s.carrier || ''}`
    ).join('\n');
    const blob = new Blob([header + rows], { type: 'text/csv' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'shipments.csv';
    a.click();
  }

  goToPage(p: number) { if (p >= 0 && p < this.totalPages) { this.currentPage = p; this.loadShipments(); } }
  onPageSizeChange(e: Event) { this.pageSize = +(e.target as HTMLSelectElement).value; this.currentPage = 0; this.loadShipments(); }
}
