// Token tabanlı kart yönetim servisi
// GÜVENLİK: CVV ve tam kart numarası Angular'a HİÇ gelmez
// GÜVENLİK: Stripe Elements iframe'i üzerinden doğrudan Stripe'a gönderilir
// GÜVENLİK: Bu serviste sadece son 4 hane ve kart meta bilgisi tutulur
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { PaymentCard, PaymentCardCreate } from '../models/payment-card.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class CardService {
  private apiUrl = `${environment.apiUrl}/api/payments/cards`;
  private cardsSignal = signal<PaymentCard[]>([]);

  constructor(private http: HttpClient, private authService: AuthService) {
    this.authService.currentUser$.subscribe(user => {
      if (user && this.authService.isIndividualUser()) {
        this.loadCards(user.id!);
      } else {
        this.cardsSignal.set([]);
      }
    });
  }

  getCards(): PaymentCard[] {
    return this.cardsSignal();
  }

  private loadCards(userId: number) {
    this.http.get<PaymentCard[]>(`${this.apiUrl}/${userId}`).subscribe({
      next: cards => this.cardsSignal.set(cards),
      error: err => console.error('Kart yükleme hatası:', err)
    });
  }

  // Token tabanlı kart ekleme
  // GÜVENLİK: Sadece Stripe token (pm_xxx) ve son 4 hane gönderilir
  addCard(card: PaymentCardCreate) {
    return this.http.post<PaymentCard>(`${environment.apiUrl}/api/payments/cards/save`, card).subscribe({
      next: (newCard) => {
        this.cardsSignal.update(cards => [...cards, newCard]);
      },
      error: err => console.error('Kart ekleme hatası:', err)
    });
  }

  deleteCard(cardId: number) {
    const user = this.authService.getUser();
    if (!user) return;
    this.http.delete(`${this.apiUrl}/${cardId}?userId=${user.id}`).subscribe({
      next: () => {
        this.cardsSignal.update(cards => cards.filter(c => c.id !== cardId));
      },
      error: err => console.error('Kart silme hatası:', err)
    });
  }

  refreshCards() {
    const user = this.authService.getUser();
    if (user) {
      this.loadCards(user.id!);
    }
  }
}
