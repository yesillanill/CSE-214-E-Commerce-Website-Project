import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { CardService } from '../../../core/services/card.service';
import { HttpClient } from '@angular/common/http';
import Swal from 'sweetalert2';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TranslateModule],
  templateUrl: './checkout.html',
  styleUrl: './checkout.scss',
})
export class Checkout implements OnInit {
  // Address
  addressOption: 'registered' | 'custom' = 'registered';
  registeredAddress = '';
  customAddress = {
    street: '',
    city: '',
    postalCode: '',
    country: ''
  };

  // Payment
  paymentMethod: 'CASH_ON_DELIVERY' | 'CREDIT_CARD' = 'CREDIT_CARD';
  cardOption: 'existing' | 'new' = 'existing';
  selectedCardId: number | null = null;
  newCard = {
    cardHolderName: '',
    cardNumber: '',
    expiryMonth: 1,
    expiryYear: 2026,
    cvv: ''
  };
  installments: number = 1;

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
    // If cart is empty, redirect back
    if (this.cart.getCart().length === 0) {
      this.router.navigate(['/cart']);
      return;
    }

    // Load registered address from profile
    const user = this.auth.getUser();
    if (user) {
      this.auth.getProfile(user.id).subscribe({
        next: (profile) => {
          const parts = [profile.street, profile.city, profile.postalCode, profile.country]
            .filter(p => p && p.trim());
          this.registeredAddress = parts.join(', ');
          this.trySelectFirstCard();
          this.isLoaded = true;
          this.cdr.detectChanges();
        }
      });
    }
  }

  private trySelectFirstCard() {
    const cards = this.cardService.getCards();
    if (cards.length > 0 && !this.selectedCardId) {
      this.selectedCardId = cards[0].id!;
    }
    // If cards haven't loaded yet, retry after a short delay
    if (cards.length === 0) {
      setTimeout(() => {
        const retryCards = this.cardService.getCards();
        if (retryCards.length > 0 && !this.selectedCardId) {
          this.selectedCardId = retryCards[0].id!;
          this.cdr.detectChanges();
        }
      }, 1000);
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
    if (this.paymentMethod === 'CREDIT_CARD') {
      if (this.cardOption === 'existing' && !this.selectedCardId) return false;
      if (this.cardOption === 'new' && (!this.newCard.cardNumber || !this.newCard.cvv)) return false;
    }
    return true;
  }

  placeOrder() {
    if (!this.canPlaceOrder() || this.isProcessing) return;
    this.isProcessing = true;

    const user = this.auth.getUser();
    if (!user) return;

    const payload: any = {
      userId: user.id,
      shippingAddress: this.getShippingAddress(),
      paymentMethod: this.paymentMethod,
      installments: this.paymentMethod === 'CREDIT_CARD' ? this.installments : 1
    };

    if (this.paymentMethod === 'CREDIT_CARD' && this.cardOption === 'existing') {
      payload.cardId = this.selectedCardId;
    }

    this.http.post<any>('http://localhost:8080/api/orders/checkout', payload).subscribe({
      next: () => {
        this.cart.refreshCart();
        Swal.fire({
          icon: 'success',
          title: this.translate.instant('CHECKOUT.ORDER_SUCCESS_TITLE'),
          text: this.translate.instant('CHECKOUT.ORDER_SUCCESS_TEXT'),
          background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
          color: getComputedStyle(document.body).getPropertyValue('--text-color').trim(),
          timer: 2500,
          showConfirmButton: false
        }).then(() => {
          this.router.navigate(['/']);
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
