import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { environment } from '../../../../environments/environment';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

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
  private searchSubject = new Subject<string>();
  sortBy = 'storeName';
  sortDir = 'asc';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(term => {
      this.searchTerm = term;
      this.currentPage = 0;
      this.loadStores();
    });
  }

  ngOnInit() { this.loadStores(); }

  loadStores() {
    this.isLoading = true;
    let url = `${environment.apiUrl}/api/admin/stores?page=${this.currentPage}&size=${this.pageSize}&sortBy=${this.sortBy}&sortDir=${this.sortDir}`;
    if (this.searchTerm) url += `&search=${this.searchTerm}`;
    this.http.get<any>(url).subscribe({
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
    const term = (event.target as HTMLInputElement).value;
    this.searchSubject.next(term);
  }

  sort(column: string) {
    if (this.sortBy === column) { this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc'; }
    else { this.sortBy = column; this.sortDir = 'asc'; }
    this.currentPage = 0;
    this.loadStores();
  }

  getSortIcon(col: string): string {
    if (this.sortBy !== col) return '↕';
    return this.sortDir === 'asc' ? '↑' : '↓';
  }

  exportCsv() {
    let url = `${environment.apiUrl}/api/admin/stores?page=0&size=${this.totalElements || 10000}&sortBy=${this.sortBy}&sortDir=${this.sortDir}`;
    if (this.searchTerm) url += `&search=${this.searchTerm}`;
    this.http.get<any>(url).subscribe({
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
