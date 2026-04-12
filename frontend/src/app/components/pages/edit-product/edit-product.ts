import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { CanComponentDeactivate } from '../../../core/guards/pending-changes-guard';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-edit-product',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './edit-product.html',
  styleUrl: './edit-product.scss',
})
export class EditProduct implements OnInit, CanComponentDeactivate {
  productId!: number;
  isLoading = true;
  isSaving = false;
  private saved = false;

  // Form model — pre-filled with existing data, user modifies in-place
  product: any = {
    name: '',
    description: '',
    price: null,
    stock: null,
    img: ''
  };

  // Snapshot of original values to detect changes
  originalSnapshot: any = {};

  // Read-only info
  categoryName = '';
  brandName = '';

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router,
    private translate: TranslateService
  ) {}

  ngOnInit() {
    this.productId = +this.route.snapshot.params['id'];
    this.http.get<any>(`http://localhost:8080/api/inventory/product/${this.productId}`).subscribe({
      next: (p) => {
        this.product = {
          name: p.name || '',
          description: p.description || '',
          price: p.price ?? 0,
          stock: p.inventory?.stock ?? 0,
          img: p.img || ''
        };
        this.originalSnapshot = { ...this.product };
        this.categoryName = p.category?.name || '';
        this.brandName = p.brand?.name || '';
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        Swal.fire({
          icon: 'error',
          title: this.translate.instant('EDIT_PRODUCT.ERROR_TITLE'),
          text: this.translate.instant('EDIT_PRODUCT.ERROR_LOAD'),
          background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
          color: getComputedStyle(document.body).getPropertyValue('--text-color').trim()
        });
      }
    });
  }

  hasChanges(): boolean {
    return (
      this.product.name !== this.originalSnapshot.name ||
      this.product.description !== this.originalSnapshot.description ||
      this.product.price !== this.originalSnapshot.price ||
      this.product.stock !== this.originalSnapshot.stock ||
      this.product.img !== this.originalSnapshot.img
    );
  }

  onSubmit() {
    if (!this.hasChanges() || this.isSaving) return;
    this.isSaving = true;

    // Build partial payload — only include changed fields
    const payload: any = {};
    if (this.product.name !== this.originalSnapshot.name) payload.name = this.product.name;
    if (this.product.description !== this.originalSnapshot.description) payload.description = this.product.description;
    if (this.product.price !== this.originalSnapshot.price) payload.price = this.product.price;
    if (this.product.stock !== this.originalSnapshot.stock) payload.stock = this.product.stock;
    if (this.product.img !== this.originalSnapshot.img) payload.img = this.product.img;

    this.http.put<any>(`http://localhost:8080/api/inventory/${this.productId}`, payload).subscribe({
      next: () => {
        this.saved = true;
        Swal.fire({
          icon: 'success',
          title: this.translate.instant('EDIT_PRODUCT.SUCCESS_TITLE'),
          text: this.translate.instant('EDIT_PRODUCT.SUCCESS_TEXT'),
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
          title: this.translate.instant('EDIT_PRODUCT.ERROR_TITLE'),
          text: this.translate.instant('EDIT_PRODUCT.ERROR_UPDATE'),
          background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
          color: getComputedStyle(document.body).getPropertyValue('--text-color').trim()
        });
      }
    });
  }

  goBack() {
    this.router.navigate(['/inventory']);
  }

  canDeactivate(): Promise<boolean> | boolean {
    if (this.saved || !this.hasChanges()) return true;
    return Swal.fire({
      title: this.translate.instant('EDIT_PRODUCT.UNSAVED_TITLE'),
      text: this.translate.instant('EDIT_PRODUCT.UNSAVED_TEXT'),
      icon: 'warning',
      showCancelButton: true,
      showDenyButton: true,
      confirmButtonText: this.translate.instant('EDIT_PRODUCT.UNSAVED_SAVE'),
      denyButtonText: this.translate.instant('EDIT_PRODUCT.UNSAVED_DISCARD'),
      cancelButtonText: this.translate.instant('EDIT_PRODUCT.UNSAVED_STAY'),
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
    if (this.hasChanges() && !this.saved) {
      $event.preventDefault();
      $event.returnValue = 'You have unsaved changes.';
    }
  }
}
