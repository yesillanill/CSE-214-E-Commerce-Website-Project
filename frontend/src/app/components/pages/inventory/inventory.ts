import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, DecimalPipe],
  templateUrl: './inventory.html',
  styleUrl: './inventory.scss',
})
export class Inventory implements OnInit {
  products: any[] = [];
  isLoading = true;
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  pageSizeOptions = [10, 25, 50];
  storeId: number | null = null;
  searchTerm = '';
  sortBy = 'name';
  sortDir = 'asc';

  constructor(private http: HttpClient, private auth: AuthService, private router: Router) {}

  ngOnInit() {
    const user = this.auth.getUser();
    if (user) {
      this.http.get<any>(`http://localhost:8080/api/stores/my-store?userId=${user.id}`).subscribe({
        next: (store: any) => {
          if (store && store.id) {
            this.storeId = store.id;
            this.loadProducts();
          } else { this.isLoading = false; }
        },
        error: () => { this.isLoading = false; }
      });
    }
  }

  loadProducts() {
    if (!this.storeId) return;
    this.isLoading = true;
    let url = `http://localhost:8080/api/inventory?storeId=${this.storeId}&page=${this.currentPage}&size=${this.pageSize}&sortBy=${this.sortBy}&sortDir=${this.sortDir}`;
    if (this.searchTerm) url += `&search=${this.searchTerm}`;

    this.http.get<any>(url).subscribe({
      next: (res) => {
        this.products = res.content || [];
        this.totalPages = res.totalPages || 0;
        this.totalElements = res.totalElements || 0;
        this.isLoading = false;
      },
      error: () => { this.products = []; this.isLoading = false; }
    });
  }

  onSearch(event: Event) {
    this.searchTerm = (event.target as HTMLInputElement).value;
    this.currentPage = 0;
    this.loadProducts();
  }

  sort(column: string) {
    if (this.sortBy === column) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else { this.sortBy = column; this.sortDir = 'asc'; }
    this.currentPage = 0;
    this.loadProducts();
  }

  getSortIcon(col: string): string {
    if (this.sortBy !== col) return '↕';
    return this.sortDir === 'asc' ? '↑' : '↓';
  }

  exportCsv() {
    let url = `http://localhost:8080/api/inventory?storeId=${this.storeId}&page=0&size=${this.totalElements || 10000}&sortBy=${this.sortBy}&sortDir=${this.sortDir}`;
    if (this.searchTerm) url += `&search=${this.searchTerm}`;
    this.http.get<any>(url).subscribe({
      next: (res) => {
        const allProducts = res.content || [];
        const header = 'ID,Name,Category,Stock,Price,Rating\n';
        const rows = allProducts.map((p: any) =>
          `${p.id},"${p.name}",${p.category?.name || ''},${p.inventory?.stock || 0},${p.price},${p.rating}`
        ).join('\n');
        this.downloadCsv(header + rows, 'inventory.csv');
      }
    });
  }

  private downloadCsv(content: string, filename: string) {
    const blob = new Blob([content], { type: 'text/csv' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = filename;
    a.click();
  }

  addProduct() { this.router.navigate(['/inventory/add']); }
  editProduct(id: number) { this.router.navigate(['/inventory/edit', id]); }

  deleteProduct(id: number, name: string) {
    Swal.fire({
      title: 'Delete Product', text: `Are you sure you want to delete "${name}"?`,
      icon: 'warning', showCancelButton: true, confirmButtonText: 'Delete', confirmButtonColor: '#ff4d4d',
    }).then((r) => {
      if (r.isConfirmed) {
        this.http.delete<any>(`http://localhost:8080/api/inventory/${id}`).subscribe({
          next: () => { this.products = this.products.filter(p => p.id !== id); Swal.fire('Deleted!', 'Product deleted.', 'success'); },
          error: () => Swal.fire('Error', 'Could not delete.', 'error')
        });
      }
    });
  }

  goToPage(page: number) { if (page >= 0 && page < this.totalPages) { this.currentPage = page; this.loadProducts(); } }
  onPageSizeChange(event: Event) { this.pageSize = +(event.target as HTMLSelectElement).value; this.currentPage = 0; this.loadProducts(); }
}
