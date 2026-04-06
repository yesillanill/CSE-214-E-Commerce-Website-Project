import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { PaymentCard, PaymentCardCreate } from '../models/payment-card.model';

@Injectable({
  providedIn: 'root',
})
export class CardService {
  private apiUrl = 'http://localhost:8080/api/cards';
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
      error: err => console.error('Error loading cards:', err)
    });
  }

  addCard(card: PaymentCardCreate) {
    return this.http.post<PaymentCard>(this.apiUrl, card).subscribe({
      next: (newCard) => {
        this.cardsSignal.update(cards => [...cards, newCard]);
      },
      error: err => console.error('Error adding card:', err)
    });
  }

  deleteCard(cardId: number) {
    const user = this.authService.getUser();
    if (!user) return;
    this.http.delete(`${this.apiUrl}/${cardId}?userId=${user.id}`).subscribe({
      next: () => {
        this.cardsSignal.update(cards => cards.filter(c => c.id !== cardId));
      },
      error: err => console.error('Error deleting card:', err)
    });
  }

  refreshCards() {
    const user = this.authService.getUser();
    if (user) {
      this.loadCards(user.id!);
    }
  }
}
