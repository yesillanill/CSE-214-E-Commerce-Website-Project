import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, DatePipe],
  templateUrl: './audit-logs.html',
  styleUrl: './audit-logs.scss',
})
export class AuditLogs implements OnInit {
  logs: any[] = [];
  isLoading = true;
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  pageSizeOptions = [10, 25, 50];

  // Filters
  actionFilter = '';
  roleFilter = '';
  startDate = '';
  endDate = '';

  actions = [
    'USER_LOGIN','USER_LOGOUT','USER_REGISTER','USER_SUSPENDED','USER_ACTIVATED','USER_DELETED',
    'PRODUCT_CREATED','PRODUCT_UPDATED','PRODUCT_DELETED',
    'ORDER_CREATED','ORDER_STATUS_UPDATED',
    'STORE_OPENED','STORE_CLOSED',
    'PAYMENT_PROCESSED','SHIPMENT_UPDATED'
  ];

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.loadLogs(); }

  loadLogs() {
    this.isLoading = true;
    let url = `http://localhost:8080/api/admin/audit-logs?page=${this.currentPage}&size=${this.pageSize}`;
    if (this.actionFilter) url += `&action=${this.actionFilter}`;
    if (this.roleFilter) url += `&userRole=${this.roleFilter}`;
    if (this.startDate) url += `&startDate=${this.startDate}`;
    if (this.endDate) url += `&endDate=${this.endDate}`;

    this.http.get<any>(url).subscribe({
      next: (res) => {
        this.logs = res.content || [];
        this.totalPages = res.totalPages || 0;
        this.totalElements = res.totalElements || 0;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.logs = []; this.isLoading = false; this.cdr.markForCheck(); }
    });
  }

  applyFilters() {
    this.currentPage = 0;
    this.loadLogs();
  }

  clearFilters() {
    this.actionFilter = '';
    this.roleFilter = '';
    this.startDate = '';
    this.endDate = '';
    this.currentPage = 0;
    this.loadLogs();
  }

  exportCsv() {
    let url = `http://localhost:8080/api/admin/audit-logs/export?`;
    if (this.actionFilter) url += `action=${this.actionFilter}&`;
    if (this.roleFilter) url += `userRole=${this.roleFilter}&`;
    if (this.startDate) url += `startDate=${this.startDate}&`;
    if (this.endDate) url += `endDate=${this.endDate}&`;

    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'audit_logs.csv';
        a.click();
      }
    });
  }

  goToPage(page: number) { if (page >= 0 && page < this.totalPages) { this.currentPage = page; this.loadLogs(); } }
  onPageSizeChange(event: Event) { this.pageSize = +(event.target as HTMLSelectElement).value; this.currentPage = 0; this.loadLogs(); }
}
