import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-stores',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, DecimalPipe],
  templateUrl: './stores.html',
  styleUrl: './stores.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Stores implements OnInit {
  stores: any[] = [];
  isLoading = true;
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  pageSizeOptions = [10, 25, 50];
  searchTerm = '';
  sortBy = 'storeName';
  sortDir = 'asc';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.loadStores(); }

  loadStores() {
    this.isLoading = true;
    this.http.get<any>(`${environment.apiUrl}/api/admin/stores?page=${this.currentPage}&size=${this.pageSize}`).subscribe({
      next: (res) => {
        this.stores = res.content || [];
        this.totalPages = res.totalPages || 0;
        this.totalElements = res.totalElements || 0;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.stores = []; this.isLoading = false; this.cdr.markForCheck(); }
    });
  }

  onSearch(event: Event) {
    this.searchTerm = (event.target as HTMLInputElement).value.toLowerCase();
    this.currentPage = 0;
    this.loadStores();
  }

  sort(column: string) {
    if (this.sortBy === column) { this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc'; }
    else { this.sortBy = column; this.sortDir = 'asc'; }
    this.stores.sort((a: any, b: any) => {
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
    this.http.get<any>(`${environment.apiUrl}/api/admin/stores?page=0&size=${this.totalElements || 10000}`).subscribe({
      next: (res) => {
        const allStores = res.content || [];
        const header = 'ID,Store Name,Company,Tax Number,Revenue\n';
        const rows = allStores.map((s: any) =>
          `${s.id},"${s.storeName}","${s.companyName || ''}",${s.taxNumber || ''},${s.totalRevenue || 0}`
        ).join('\n');
        const blob = new Blob([header + rows], { type: 'text/csv' });
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'stores.csv';
        a.click();
      }
    });
  }

  goToPage(p: number) { if (p >= 0 && p < this.totalPages) { this.currentPage = p; this.loadStores(); } }
  onPageSizeChange(e: Event) { this.pageSize = +(e.target as HTMLSelectElement).value; this.currentPage = 0; this.loadStores(); }
}
