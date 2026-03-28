import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { CanComponentDeactivate } from '../../../core/guards/pending-changes-guard';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-add-product',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './add-product.html',
  styleUrl: './add-product.scss',
})
export class AddProduct implements OnInit, CanComponentDeactivate {
  product: any = {
    name: '',
    description: '',
    price: null,
    stock: null,
    categoryId: null,
    brandId: null,
    img: ''
  };

  categories: any[] = [];
  brands: any[] = [];
  storeId: number | null = null;
  isSaving = false;
  private saved = false;

  constructor(
    private http: HttpClient,
    private auth: AuthService,
    private router: Router,
    private translate: TranslateService
  ) {}

  ngOnInit() {
    const user = this.auth.getUser();
    if (user) {
      this.http.get<any>(`http://localhost:8080/api/stores/my-store?userId=${user.id}`).subscribe({
        next: (store) => {
          if (store?.id) this.storeId = store.id;
        }
      });
    }

    this.http.get<any[]>('http://localhost:8080/api/inventory/categories').subscribe({
      next: (cats) => this.categories = cats || []
    });
    this.http.get<any[]>('http://localhost:8080/api/inventory/brands').subscribe({
      next: (brands) => this.brands = brands || []
    });
  }

  onSubmit() {
    if (!this.storeId || this.isSaving) return;
    this.isSaving = true;

    this.http.post<any>(`http://localhost:8080/api/inventory?storeId=${this.storeId}`, this.product).subscribe({
      next: () => {
        this.saved = true;
        Swal.fire({
          icon: 'success',
          title: 'Product Added!',
          text: 'Product has been successfully created.',
          background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
          color: getComputedStyle(document.body).getPropertyValue('--text-color').trim(),
          timer: 1500,
          showConfirmButton: false
        }).then(() => this.router.navigate(['/inventory']));
      },
      error: () => {
        this.isSaving = false;
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'Could not create the product.',
          background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
          color: getComputedStyle(document.body).getPropertyValue('--text-color').trim()
        });
      }
    });
  }

  goBack() {
    this.router.navigate(['/inventory']);
  }

  private isDirty(): boolean {
    return !this.saved && (
      !!this.product.name ||
      !!this.product.description ||
      this.product.price != null ||
      this.product.stock != null ||
      this.product.categoryId != null ||
      this.product.brandId != null ||
      !!this.product.img
    );
  }

  canDeactivate(): Promise<boolean> | boolean {
    if (!this.isDirty()) return true;
    return Swal.fire({
      title: 'Unsaved Changes',
      text: 'You have unsaved changes. What would you like to do?',
      icon: 'warning',
      showCancelButton: true,
      showDenyButton: true,
      confirmButtonText: 'Save',
      denyButtonText: 'Discard',
      cancelButtonText: 'Stay',
      background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
      color: getComputedStyle(document.body).getPropertyValue('--text-color').trim()
    }).then(result => {
      if (result.isConfirmed) {
        return new Promise<boolean>((resolve) => {
          this.onSubmit();
          resolve(true);
        });
      }
      if (result.isDenied) return true;
      return false;
    });
  }

  @HostListener('window:beforeunload', ['$event'])
  unloadNotification($event: BeforeUnloadEvent) {
    if (this.isDirty()) {
      $event.preventDefault();
      $event.returnValue = 'You have unsaved changes.';
    }
  }
}
