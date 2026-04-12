import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { SupportService } from '../../../core/services/support.service';
import { SupportTicket } from '../../../core/models/support.model';
import { LoadingSpinner } from '../../layout/loading-spinner/loading-spinner';

@Component({
  selector: 'app-support-requests',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TranslateModule, DatePipe, LoadingSpinner],
  templateUrl: './support-requests.html',
  styleUrl: './support-requests.scss',
})
export class SupportRequests implements OnInit {
  tickets: SupportTicket[] = [];
  filteredTickets: SupportTicket[] = [];
  isLoading = true;
  expandedTicketId: number | null = null;
  statusFilter = 'ALL';
  responseText: { [ticketId: number]: string } = {};

  constructor(private supportService: SupportService, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.loadTickets();
  }

  loadTickets() {
    this.isLoading = true;
    this.supportService.getAllTickets().subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.applyFilter();
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.isLoading = false; this.cdr.markForCheck(); },
    });
  }

  applyFilter() {
    if (this.statusFilter === 'ALL') {
      this.filteredTickets = this.tickets;
    } else {
      this.filteredTickets = this.tickets.filter((t) => t.status === this.statusFilter);
    }
  }

  toggleExpand(ticketId: number) {
    this.expandedTicketId = this.expandedTicketId === ticketId ? null : ticketId;
  }

  respondToTicket(ticketId: number, newStatus?: string) {
    const text = this.responseText[ticketId];
    if (!text?.trim()) return;

    this.supportService.respondToTicket(ticketId, text, newStatus).subscribe({
      next: (resp) => {
        const ticket = this.tickets.find((t) => t.id === ticketId);
        if (ticket) {
          ticket.responses.push(resp);
          if (newStatus) ticket.status = newStatus;
        }
        this.responseText[ticketId] = '';
        this.applyFilter();
        this.cdr.markForCheck();
      },
    });
  }

  updateStatus(ticketId: number, status: string) {
    this.supportService.updateTicketStatus(ticketId, status).subscribe({
      next: (updated) => {
        const ticket = this.tickets.find((t) => t.id === ticketId);
        if (ticket) ticket.status = updated.status;
        this.applyFilter();
        this.cdr.markForCheck();
      },
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'OPEN': return 'status-open';
      case 'RESOLVED': return 'status-resolved';
      case 'NO_ISSUE_FOUND': return 'status-no-issue';
      default: return '';
    }
  }
}
