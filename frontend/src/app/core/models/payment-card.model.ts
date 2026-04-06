export interface PaymentCard {
  id?: number;
  cardHolderName: string;
  maskedCardNumber: string;
  expiryMonth: number;
  expiryYear: number;
  cardType: string;
}

export interface PaymentCardCreate {
  userId: number;
  cardHolderName: string;
  cardNumber: string;
  expiryMonth: number;
  expiryYear: number;
  cvv: string;
  cardType?: string;
}
