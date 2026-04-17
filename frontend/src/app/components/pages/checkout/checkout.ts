import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { CardService } from '../../../core/services/card.service';
import { HttpClient } from '@angular/common/http';
import Swal from 'sweetalert2';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TranslateModule],
  templateUrl: './checkout.html',
  styleUrl: './checkout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Checkout implements OnInit {
  // Adres
  addressOption: 'registered' | 'custom' = 'registered';
  registeredAddress = '';
  customAddress = {
    street: '',
    city: '',
    postalCode: '',
    country: ''
  };

  // Ödeme yöntemi seçimi — artık ayrı ödeme sayfasına yönlendirme yapılıyor
  paymentMethod: 'STRIPE' | 'PAYPAL' | 'CRYPTO' | 'COD' = 'STRIPE';

  isProcessing = false;
  isLoaded = false;

  constructor(
    public cart: CartService,
    public auth: AuthService,
    public cardService: CardService,
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private translate: TranslateService
  ) {}

  ngOnInit() {
    if (this.cart.getCart().length === 0) {
      this.router.navigate(['/cart']);
      return;
    }

    const user = this.auth.getUser();
    if (user) {
      this.auth.getProfile(user.id).subscribe({
        next: (profile) => {
          const parts = [profile.street, profile.city, profile.postalCode, profile.country]
            .filter(p => p && p.trim());
          this.registeredAddress = parts.join(', ');
          this.isLoaded = true;
          this.cdr.detectChanges();
        }
      });
    }
  }

  getShippingAddress(): string {
    if (this.addressOption === 'registered') {
      return this.registeredAddress;
    }
    const parts = [this.customAddress.street, this.customAddress.city, this.customAddress.postalCode, this.customAddress.country]
      .filter(p => p && p.trim());
    return parts.join(', ');
  }

  canPlaceOrder(): boolean {
    if (this.cart.getCart().length === 0) return false;
    const address = this.getShippingAddress();
    if (!address || address.trim() === '') return false;
    return true;
  }

  // Sipariş oluştur ve ödeme sayfasına yönlendir
  placeOrder() {
    if (!this.canPlaceOrder() || this.isProcessing) return;
    this.isProcessing = true;

    const user = this.auth.getUser();
    if (!user) return;

    const payload: any = {
      userId: user.id,
      shippingAddress: this.getShippingAddress(),
      paymentMethod: this.paymentMethod === 'COD' ? 'CASH_ON_DELIVERY' : 'CREDIT_CARD'
    };

    this.http.post<any>(`${environment.apiUrl}/api/orders/checkout`, payload).subscribe({
      next: (order) => {
        // Sipariş oluşturuldu, ödeme sayfasına yönlendir
        this.router.navigate(['/payment'], {
          queryParams: {
            orderId: order.id,
            amount: this.cart.getTotal()
          }
        });
      },
      error: (err) => {
        this.isProcessing = false;
        this.cdr.markForCheck();
        Swal.fire({
          icon: 'error',
          title: this.translate.instant('CHECKOUT.ORDER_ERROR_TITLE'),
          text: err.error?.message || this.translate.instant('CHECKOUT.ORDER_ERROR_TEXT'),
          background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
          color: getComputedStyle(document.body).getPropertyValue('--text-color').trim()
        });
        console.error('Checkout error:', err);
      }
    });
  }
}
