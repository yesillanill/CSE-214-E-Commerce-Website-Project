// Ödeme servisi — Tüm backend ödeme endpoint'lerine HTTP istekleri
// GÜVENLİK: Kart bilgisi hiçbir zaman bu servis üzerinden geçmez
// GÜVENLİK: Sadece Stripe token (pm_xxx) backend'e gönderilir
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  PaymentRequest,
  PaymentResponse,
  PaymentCard,
  PaymentCardCreate,
  PaymentHistory
} from '../models/payment-card.model';

@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  private apiUrl = 'http://localhost:8080/api/payments';

  constructor(private http: HttpClient) {}

  // Ödeme başlat (Stripe, PayPal, Crypto, COD)
  createPayment(request: PaymentRequest): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(`${this.apiUrl}/create`, request).pipe(
      catchError(this.handleError)
    );
  }

  // Stripe token ile kart kaydet
  saveCard(request: PaymentCardCreate): Observable<PaymentCard> {
    return this.http.post<PaymentCard>(`${this.apiUrl}/cards/save`, request).pipe(
      catchError(this.handleError)
    );
  }

  // Kullanıcının kayıtlı kartlarını getir
  // GÜVENLİK: Token bilgisi dönmez, sadece son 4 hane + kart tipi + expiry
  getUserCards(userId: number): Observable<PaymentCard[]> {
    return this.http.get<PaymentCard[]>(`${this.apiUrl}/cards/${userId}`).pipe(
      catchError(this.handleError)
    );
  }

  // Kullanıcının ödeme geçmişi
  getPaymentHistory(userId: number): Observable<PaymentHistory[]> {
    return this.http.get<PaymentHistory[]>(`${this.apiUrl}/history/${userId}`).pipe(
      catchError(this.handleError)
    );
  }

  // Siparişe ait ödeme detayı
  getPaymentByOrder(orderId: number): Observable<PaymentHistory> {
    return this.http.get<PaymentHistory>(`${this.apiUrl}/order/${orderId}`).pipe(
      catchError(this.handleError)
    );
  }

  // PayPal ödeme onayı (capture)
  capturePayPalOrder(paypalOrderId: string, orderId: number): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(
      `${this.apiUrl}/paypal/capture?paypalOrderId=${paypalOrderId}&orderId=${orderId}`, {}
    ).pipe(catchError(this.handleError));
  }

  // Kapıda ödeme teslimat onayı
  confirmCodDelivery(paymentId: number): Observable<PaymentResponse> {
    return this.http.patch<PaymentResponse>(`${this.apiUrl}/${paymentId}/cod-confirm`, {}).pipe(
      catchError(this.handleError)
    );
  }

  // PayPal Client ID'yi backend'den al
  // GÜVENLİK: Backend'den sadece client ID döner, secret dönmez
  getPayPalClientId(): Observable<{ clientId: string }> {
    return this.http.get<{ clientId: string }>(`${this.apiUrl}/config/paypal-client-id`).pipe(
      catchError(this.handleError)
    );
  }

  // COD maksimum tutar limitini al
  getCodLimit(): Observable<{ maxAmount: number }> {
    return this.http.get<{ maxAmount: number }>(`${this.apiUrl}/config/cod-limit`).pipe(
      catchError(this.handleError)
    );
  }

  // Kart sil
  deleteCard(cardId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/cards/${cardId}?userId=${userId}`).pipe(
      catchError(this.handleError)
    );
  }

  // HTTP hata yakalama
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Bir hata oluştu';
    if (error.error) {
      if (typeof error.error === 'string') {
        errorMessage = error.error;
      } else if (error.error.message) {
        errorMessage = error.error.message;
      }
    }
    console.error('Ödeme servisi hatası:', errorMessage, error);
    return throwError(() => ({ message: errorMessage, status: error.status }));
  }
}
