import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SupportTicket, TicketCreate, TicketResponse } from '../models/support.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class SupportService {
  private api = `${environment.apiUrl}/api/support`;

  constructor(private http: HttpClient) {}

  createTicket(ticket: TicketCreate): Observable<SupportTicket> {
    return this.http.post<SupportTicket>(this.api, ticket);
  }

  getMyTickets(): Observable<SupportTicket[]> {
    return this.http.get<SupportTicket[]>(`${this.api}/my-tickets`);
  }

  getMyTicket(ticketId: number): Observable<SupportTicket> {
    return this.http.get<SupportTicket>(`${this.api}/my-tickets/${ticketId}`);
  }

  getAllTickets(): Observable<SupportTicket[]> {
    return this.http.get<SupportTicket[]>(`${this.api}/admin/tickets`);
  }

  getTicket(ticketId: number): Observable<SupportTicket> {
    return this.http.get<SupportTicket>(`${this.api}/admin/tickets/${ticketId}`);
  }

  respondToTicket(ticketId: number, message: string, status?: string): Observable<TicketResponse> {
    return this.http.post<TicketResponse>(`${this.api}/admin/tickets/${ticketId}/respond`, { message, status });
  }

  updateTicketStatus(ticketId: number, status: string): Observable<SupportTicket> {
    return this.http.patch<SupportTicket>(`${this.api}/admin/tickets/${ticketId}/status`, { status });
  }
}
