export interface SupportTicket {
  id: number;
  userId: number;
  userName: string;
  subject: string;
  message: string;
  type: string;
  status: string;
  productId: number | null;
  productName: string | null;
  reviewId: number | null;
  createdAt: string;
  responses: TicketResponse[];
}

export interface TicketResponse {
  id: number;
  adminName: string;
  message: string;
  createdAt: string;
}

export interface TicketCreate {
  subject: string;
  message: string;
  type: string;
  productId?: number;
  reviewId?: number;
}
