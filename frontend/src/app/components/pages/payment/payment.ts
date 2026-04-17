// Çoklu ödeme yöntemi sayfası
// Stripe, PayPal Sandbox, Coinbase Commerce (Kripto) ve Kapıda Ödeme
// GÜVENLİK: Kart numarası ve CVV Angular component'ine HİÇ gelmez
// GÜVENLİK: Stripe Elements iframe içinde çalışır, hassas veri Angular'a ulaşmaz
// GÜVENLİK: Backend'e sadece Stripe token (pm_xxx) gönderilir
import { Component, OnInit, OnDestroy, ChangeDetectorRef, NgZone, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { PaymentService } from '../../../core/services/payment.service';
import { CardService } from '../../../core/services/card.service';
import { CartService } from '../../../core/services/cart.service';
import { PaymentCard, PaymentRequest, PaymentResponse, PaymentHistory } from '../../../core/models/payment-card.model';
import { environment } from '../../../../environments/environment';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import Swal from 'sweetalert2';

// Stripe JS global deklarasyonu
declare var Stripe: any;

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TranslateModule],
  templateUrl: './payment.html',
  styleUrl: './payment.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentPage implements OnInit, OnDestroy {

  // Aktif sekme
  activeTab: 'STRIPE' | 'PAYPAL' | 'CRYPTO' | 'COD' = 'STRIPE';

  // Stripe
  stripe: any;
  cardElement: any;
  elements: any;
  stripeReady = false;
  cardHolderName = '';
  saveCardChecked = false;

  // Kayıtlı kartlar
  savedCards: PaymentCard[] = [];
  selectedCardId: number | null = null;
  useExistingCard = false;

  // PayPal
  paypalClientId = '';
  paypalReady = false;
  paypalButtonRendered = false;

  // Kapıda Ödeme
  deliveryNotes = '';
  codMaxAmount = 500;

  // Ödeme geçmişi
  paymentHistory: PaymentHistory[] = [];
  showHistory = false;

  // Genel
  isProcessing = false;
  isLoading = true;
  orderAmount = 0;
  orderId: number | null = null;
  currency = 'USD';

  constructor(
    public auth: AuthService,
    private paymentService: PaymentService,
    public cardService: CardService,
    public cart: CartService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private zone: NgZone,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    const user = this.auth.getUser();
    if (!user) {
      this.router.navigate(['/auth']);
      return;
    }

    // URL'den orderId ve amount al
    this.route.queryParams.subscribe(params => {
      if (params['orderId']) {
        this.orderId = +params['orderId'];
      }
      if (params['amount']) {
        this.orderAmount = +params['amount'];
      }

      // PayPal return handling
      if (params['paypal'] === 'success' && params['token']) {
        this.handlePayPalReturn(params['token']);
      }
    });

    // Sepetten geliyorsa tutarı al
    if (this.orderAmount === 0 && this.cart.getTotal() > 0) {
      this.orderAmount = this.cart.getTotal();
    }

    // Kayıtlı kartları yükle
    this.loadSavedCards();

    // COD limitini al
    this.paymentService.getCodLimit().subscribe({
      next: (res) => { this.codMaxAmount = res.maxAmount; },
      error: () => { this.codMaxAmount = 500; }
    });

    // PayPal Client ID'yi al
    this.paymentService.getPayPalClientId().subscribe({
      next: (res) => {
        this.paypalClientId = res.clientId;
      },
      error: (err) => console.error('PayPal client ID alınamadı:', err)
    });

    // Ödeme geçmişini yükle
    this.loadPaymentHistory();

    // Stripe.js yükle
    this.loadStripeJs();

    this.isLoading = false;
  }

  ngOnDestroy(): void {
    if (this.cardElement) {
      this.cardElement.destroy();
    }
  }

  // === SEKME YÖNETİMİ ===

  selectTab(tab: 'STRIPE' | 'PAYPAL' | 'CRYPTO' | 'COD'): void {
    this.activeTab = tab;
    if (tab === 'STRIPE' && !this.stripeReady) {
      setTimeout(() => this.mountStripeElements(), 100);
    }
    if (tab === 'PAYPAL' && !this.paypalButtonRendered) {
      setTimeout(() => this.loadPayPalSdk(), 100);
    }
  }

  // === STRIPE ===

  // Stripe.js CDN'den yükle
  // GÜVENLİK: Kart numarası ve CVV Stripe Elements iframe'i içinde kalır
  private loadStripeJs(): void {
    if (typeof Stripe !== 'undefined') {
      this.initStripe();
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://js.stripe.com/v3/';
    script.onload = () => this.initStripe();
    document.head.appendChild(script);
  }

  private initStripe(): void {
    this.stripe = Stripe(environment.stripePublishableKey);
    this.elements = this.stripe.elements();
    setTimeout(() => this.mountStripeElements(), 200);
  }

  private mountStripeElements(): void {
    if (!this.stripe || !this.elements) return;

    const cardContainer = document.getElementById('stripe-card-element');
    if (!cardContainer) return;

    // Mevcut element varsa yok et
    if (this.cardElement) {
      this.cardElement.destroy();
    }

    // GÜVENLİK: Stripe Elements iframe oluşturur — kart bilgisi Angular'a hiç gelmez
    this.cardElement = this.elements.create('card', {
      style: {
        base: {
          color: '#e2e8f0',
          fontFamily: '"Inter", "Segoe UI", sans-serif',
          fontSize: '16px',
          '::placeholder': { color: '#64748b' },
          iconColor: '#818cf8'
        },
        invalid: {
          color: '#f87171',
          iconColor: '#f87171'
        }
      },
      hidePostalCode: true
    });

    this.cardElement.mount('#stripe-card-element');
    this.stripeReady = true;
    this.cdr.detectChanges();
  }

  // Stripe ile ödeme yap
  // GÜVENLİK: stripe.createPaymentMethod() ile token alınır, kart bilgisi backend'e GİTMEZ
  async payWithStripe(): Promise<void> {
    if (this.isProcessing) return;
    this.isProcessing = true;

    try {
      // Kayıtlı kart ile ödeme
      if (this.useExistingCard && this.selectedCardId) {
        const request: PaymentRequest = {
          amount: this.orderAmount,
          currency: this.currency,
          orderId: this.orderId!,
          provider: 'STRIPE',
          savedCardId: this.selectedCardId,
          description: 'E-Commerce Sipariş Ödemesi'
        };

        this.paymentService.createPayment(request).subscribe({
          next: (res) => this.handlePaymentSuccess(res),
          error: (err) => this.handlePaymentError(err)
        });
        return;
      }

      // Yeni kart ile ödeme — Stripe Elements'ten token al
      // GÜVENLİK: Kart bilgisi doğrudan Stripe'a gider, Angular'a gelmez
      const { paymentMethod, error } = await this.stripe.createPaymentMethod({
        type: 'card',
        card: this.cardElement,
        billing_details: {
          name: this.cardHolderName
        }
      });

      if (error) {
        this.showError(error.message);
        this.isProcessing = false;
        return;
      }

      // GÜVENLİK: Backend'e sadece token (pm_xxx) + son 4 hane gönderilir
      const request: PaymentRequest = {
        amount: this.orderAmount,
        currency: this.currency,
        orderId: this.orderId!,
        provider: 'STRIPE',
        cardToken: paymentMethod.id,
        cardLastFour: paymentMethod.card.last4,
        cardHolderName: this.cardHolderName,
        expiryMonth: paymentMethod.card.exp_month,
        expiryYear: paymentMethod.card.exp_year,
        cardType: paymentMethod.card.brand?.toUpperCase(),
        saveCard: this.saveCardChecked,
        description: 'E-Commerce Sipariş Ödemesi'
      };

      this.paymentService.createPayment(request).subscribe({
        next: (res) => this.handlePaymentSuccess(res),
        error: (err) => this.handlePaymentError(err)
      });

    } catch (error: any) {
      this.showError(error.message || 'Stripe ödeme hatası');
      this.isProcessing = false;
    }
  }

  // === PAYPAL ===

  private loadPayPalSdk(): void {
    if (this.paypalButtonRendered) return;
    if (!this.paypalClientId) {
      setTimeout(() => this.loadPayPalSdk(), 500);
      return;
    }

    // PayPal SDK zaten yüklüyse direkt render et
    if ((window as any).paypal) {
      this.renderPayPalButton();
      return;
    }

    const script = document.createElement('script');
    script.src = `https://www.paypal.com/sdk/js?client-id=${this.paypalClientId}&currency=USD`;
    script.onload = () => {
      this.paypalReady = true;
      this.renderPayPalButton();
    };
    document.head.appendChild(script);
  }

  private renderPayPalButton(): void {
    const container = document.getElementById('paypal-button-container');
    if (!container || this.paypalButtonRendered) return;

    (window as any).paypal.Buttons({
      createOrder: (_data: any, actions: any) => {
        // Önce backend'de PayPal order oluştur
        return new Promise((resolve, reject) => {
          const request: PaymentRequest = {
            amount: this.orderAmount,
            currency: this.currency,
            orderId: this.orderId!,
            provider: 'PAYPAL',
            description: 'E-Commerce Sipariş Ödemesi'
          };

          this.paymentService.createPayment(request).subscribe({
            next: (res) => {
              resolve(res.transactionId);
            },
            error: (err) => {
              this.showError(err.message || 'PayPal order oluşturulamadı');
              reject(err);
            }
          });
        });
      },
      onApprove: (data: any) => {
        this.zone.run(() => {
          this.isProcessing = true;
          // Backend'de PayPal order'ı capture et
          this.paymentService.capturePayPalOrder(data.orderID, this.orderId!).subscribe({
            next: (res) => this.handlePaymentSuccess(res),
            error: (err) => this.handlePaymentError(err)
          });
        });
      },
      onCancel: () => {
        this.zone.run(() => {
          this.showError('PayPal ödemesi iptal edildi');
        });
      },
      onError: (err: any) => {
        this.zone.run(() => {
          this.showError('PayPal hatası: ' + (err.message || 'Bilinmeyen hata'));
        });
      },
      style: {
        color: 'blue',
        shape: 'rect',
        label: 'pay',
        height: 45
      }
    }).render('#paypal-button-container');

    this.paypalButtonRendered = true;
  }

  private handlePayPalReturn(token: string): void {
    if (!this.orderId) return;
    this.isProcessing = true;
    this.paymentService.capturePayPalOrder(token, this.orderId).subscribe({
      next: (res) => this.handlePaymentSuccess(res),
      error: (err) => this.handlePaymentError(err)
    });
  }

  // === KRİPTO ===

  payWithCrypto(): void {
    if (this.isProcessing || !this.orderId) return;
    this.isProcessing = true;

    const request: PaymentRequest = {
      amount: this.orderAmount,
      currency: this.currency,
      orderId: this.orderId,
      provider: 'CRYPTO',
      description: 'E-Commerce Sipariş Ödemesi'
    };

    this.paymentService.createPayment(request).subscribe({
      next: (res) => {
        if (res.checkoutUrl) {
          // Coinbase checkout sayfasına yönlendir
          window.location.href = res.checkoutUrl;
        } else {
          this.handlePaymentSuccess(res);
        }
      },
      error: (err) => this.handlePaymentError(err)
    });
  }

  // === KAPIDA ÖDEME ===

  // Kapıda ödeme tutar limiti kontrolü
  // Bu kontrol hem frontend hem backend'de yapılır
  get isCodOverLimit(): boolean {
    return this.orderAmount > this.codMaxAmount;
  }

  payWithCod(): void {
    if (this.isProcessing || !this.orderId || this.isCodOverLimit) return;
    this.isProcessing = true;

    const request: PaymentRequest = {
      amount: this.orderAmount,
      currency: this.currency,
      orderId: this.orderId,
      provider: 'COD',
      deliveryNotes: this.deliveryNotes,
      description: 'Kapıda Ödeme'
    };

    this.paymentService.createPayment(request).subscribe({
      next: (res) => this.handlePaymentSuccess(res),
      error: (err) => this.handlePaymentError(err)
    });
  }

  // === ORTAK FONKSİYONLAR ===

  private loadSavedCards(): void {
    const user = this.auth.getUser();
    if (!user) return;

    this.paymentService.getUserCards(user.id!).subscribe({
      next: (cards) => {
        this.savedCards = cards;
        if (cards.length > 0) {
          this.selectedCardId = cards[0].id!;
        }
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Kart yükleme hatası:', err)
    });
  }

  loadPaymentHistory(): void {
    const user = this.auth.getUser();
    if (!user) return;

    this.paymentService.getPaymentHistory(user.id!).subscribe({
      next: (history) => {
        this.paymentHistory = history;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Ödeme geçmişi yükleme hatası:', err)
    });
  }

  toggleHistory(): void {
    this.showHistory = !this.showHistory;
    if (this.showHistory) {
      this.loadPaymentHistory();
    }
  }

  private handlePaymentSuccess(response: PaymentResponse): void {
    this.isProcessing = false;
    this.cart.refreshCart();
    this.loadPaymentHistory();

    Swal.fire({
      icon: 'success',
      title: 'Ödeme Başarılı! ✅',
      text: response.message || 'Ödemeniz başarıyla işlendi',
      background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
      color: getComputedStyle(document.body).getPropertyValue('--text-color').trim(),
      timer: 3000,
      showConfirmButton: false
    }).then(() => {
      this.router.navigate(['/orders']);
    });
  }

  private handlePaymentError(error: any): void {
    this.isProcessing = false;
    this.cdr.detectChanges();

    const message = error?.message || 'Ödeme işlemi sırasında bir hata oluştu';
    this.showError(message);
  }

  private showError(message: string): void {
    Swal.fire({
      icon: 'error',
      title: 'Ödeme Hatası',
      text: message,
      background: getComputedStyle(document.body).getPropertyValue('--card-bg').trim(),
      color: getComputedStyle(document.body).getPropertyValue('--text-color').trim()
    });
  }

  getStatusBadgeClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'SUCCESS':
      case 'COMPLETED': return 'badge-success';
      case 'PENDING': return 'badge-pending';
      case 'FAILED': return 'badge-failed';
      case 'AWAITING_DELIVERY': return 'badge-awaiting';
      default: return 'badge-default';
    }
  }

  getProviderIcon(provider: string): string {
    switch (provider?.toUpperCase()) {
      case 'STRIPE': return '💳';
      case 'PAYPAL': return '🅿️';
      case 'CRYPTO': return '₿';
      case 'COD': return '🚪';
      default: return '💰';
    }
  }
}
