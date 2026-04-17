import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import Swal from 'sweetalert2';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, DecimalPipe, DatePipe],
  templateUrl: './customers.html',
  styleUrl: './customers.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Customers implements OnInit {
  customers: any[] = [];
  isLoading = true;
  searchTerm = '';
  searchSubject = new Subject<string>();
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  pageSizeOptions = [10, 25, 50];
  sortBy = 'name';
  sortDir = 'asc';

  constructor(private http: HttpClient, private translate: TranslateService, private cdr: ChangeDetectorRef) {
    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(term => {
      this.searchTerm = term;
      this.currentPage = 0;
      this.loadCustomers();
    });
  }

  ngOnInit() { this.loadCustomers(); }

  loadCustomers() {
    this.isLoading = true;
    this.http.get<any>(`http://localhost:8080/api/admin/users?search=${this.searchTerm}&page=${this.currentPage}&size=${this.pageSize}`).subscribe({
      next: (res) => {
        this.customers = res.content || [];
        this.totalPages = res.totalPages || 0;
        this.totalElements = res.totalElements || 0;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.customers = []; this.isLoading = false; this.cdr.markForCheck(); }
    });
  }

  onSearch(event: Event) { this.searchSubject.next((event.target as HTMLInputElement).value); }

  sort(column: string) {
    if (this.sortBy === column) { this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc'; }
    else { this.sortBy = column; this.sortDir = 'asc'; }
    // Client-side sorting since backend returns all in-memory anyway
    this.customers.sort((a: any, b: any) => {
      let va = a[column], vb = b[column];
      if (va == null) va = ''; if (vb == null) vb = '';
      if (typeof va === 'string') { va = va.toLowerCase(); vb = (vb || '').toLowerCase(); }
      if (va < vb) return this.sortDir === 'asc' ? -1 : 1;
      if (va > vb) return this.sortDir === 'asc' ? 1 : -1;
      return 0;
    });
  }

  getSortIcon(col: string): string {
    if (this.sortBy !== col) return '↕';
    return this.sortDir === 'asc' ? '↑' : '↓';
  }

  exportCsv() {
    this.http.get<any>(`http://localhost:8080/api/admin/users?search=${this.searchTerm}&page=0&size=${this.totalElements || 10000}`).subscribe({
      next: (res) => {
        const allCustomers = res.content || [];
        const header = 'ID,Name,Email,Registered,Orders,Total Spend,Membership\n';
        const rows = allCustomers.map((c: any) =>
          `${c.id},"${c.name} ${c.surname}",${c.email},${c.createdAt || ''},${c.totalOrders},${c.totalSpend},${c.membershipType || ''}`
        ).join('\n');
        const blob = new Blob([header + rows], { type: 'text/csv' });
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'customers.csv';
        a.click();
      }
    });
  }

  deleteUser(userId: number, name: string) {
    Swal.fire({ title: this.translate.instant('CUSTOMERS.DELETE_TITLE'), text: `${this.translate.instant('CUSTOMERS.DELETE_CONFIRM', {name})}`, icon: 'warning', showCancelButton: true, confirmButtonText: this.translate.instant('CUSTOMERS.DELETE_BTN'), confirmButtonColor: '#ff4d4d' })
      .then((r) => {
        if (r.isConfirmed) {
          this.http.delete<any>(`http://localhost:8080/api/admin/users/${userId}`).subscribe({
            next: () => { this.customers = this.customers.filter(c => c.id !== userId); Swal.fire(this.translate.instant('CUSTOMERS.DELETED'), this.translate.instant('CUSTOMERS.DELETED_TEXT'), 'success'); },
            error: () => Swal.fire(this.translate.instant('CUSTOMERS.ERROR'), this.translate.instant('CUSTOMERS.ERROR_DELETE'), 'error')
          });
        }
      });
  }

  goToPage(p: number) { if (p >= 0 && p < this.totalPages) { this.currentPage = p; this.loadCustomers(); } }
  onPageSizeChange(e: Event) { this.pageSize = +(e.target as HTMLSelectElement).value; this.currentPage = 0; this.loadCustomers(); }
}
