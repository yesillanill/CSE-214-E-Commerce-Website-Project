// Ödeme sistemi TypeScript arayüzleri
// GÜVENLİK: PaymentCard arayüzünde token bilgisi YOKTUR
// GÜVENLİK: CVV ve tam kart numarası hiçbir arayüzde yer almaz

// Kayıtlı kart bilgisi (backend'den dönen)
// GÜVENLİK: Token asla frontend'e gönderilmez
export interface PaymentCard {
  id?: number;
  cardHolderName: string;
  lastFour: string;
  maskedCardNumber: string;
  expiryMonth: number;
  expiryYear: number;
  cardType: string;
  paymentProvider: string;
}

// Token tabanlı kart kaydetme isteği
// GÜVENLİK: CVV ve tam kart numarası bu arayüzde YOKTUR
export interface PaymentCardCreate {
  userId: number;
  cardHolderName: string;
  cardToken: string; // Stripe pm_xxx token'ı
  lastFour: string; // Sadece son 4 hane
  expiryMonth: number;
  expiryYear: number;
  cardType?: string;
  paymentProvider?: string;
}

// Ödeme başlatma isteği
export interface PaymentRequest {
  amount: number;
  currency: string;
  orderId: number;
  provider: 'STRIPE' | 'PAYPAL' | 'CRYPTO' | 'COD';
  cardToken?: string;
  cardLastFour?: string;
  cardHolderName?: string;
  expiryMonth?: number;
  expiryYear?: number;
  cardType?: string;
  deliveryNotes?: string;
  description?: string;
  savedCardId?: number;
  saveCard?: boolean;
}

// Ödeme yanıtı
export interface PaymentResponse {
  transactionId: string;
  status: string;
  providerResponse?: string;
  checkoutUrl?: string;
  approvalUrl?: string;
  message: string;
}

// Ödeme geçmişi
export interface PaymentHistory {
  id: number;
  amount: number;
  paymentMethod: string;
  paymentStatus: string;
  paymentProvider: string;
  currency: string;
  providerTransactionId: string;
  paidAt: string;
  notes?: string;
  order?: {
    id: number;
  };
}
