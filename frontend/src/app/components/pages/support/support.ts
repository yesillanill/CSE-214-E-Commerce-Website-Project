import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { SupportService } from '../../../core/services/support.service';
import { SupportTicket } from '../../../core/models/support.model';
import { LoadingSpinner } from '../../layout/loading-spinner/loading-spinner';

@Component({
  selector: 'app-support',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TranslateModule, DatePipe, LoadingSpinner],
  templateUrl: './support.html',
  styleUrl: './support.scss',
})
export class Support implements OnInit {
  tickets: SupportTicket[] = [];
  isLoading = true;
  showForm = false;
  expandedTicketId: number | null = null;

  newSubject = '';
  newMessage = '';
  submitting = false;

  constructor(private supportService: SupportService, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.loadTickets();
  }

  loadTickets() {
    this.isLoading = true;
    this.supportService.getMyTickets().subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.isLoading = false; this.cdr.markForCheck(); },
    });
  }

  toggleForm() {
    this.showForm = !this.showForm;
  }

  submitTicket() {
    if (!this.newSubject.trim() || !this.newMessage.trim() || this.submitting) return;
    this.submitting = true;
    this.supportService
      .createTicket({ subject: this.newSubject, message: this.newMessage, type: 'GENERAL' })
      .subscribe({
        next: (ticket) => {
          this.tickets.unshift(ticket);
          this.newSubject = '';
          this.newMessage = '';
          this.showForm = false;
          this.submitting = false;
          this.cdr.markForCheck();
        },
        error: () => { this.submitting = false; this.cdr.markForCheck(); },
      });
  }

  toggleExpand(ticketId: number) {
    this.expandedTicketId = this.expandedTicketId === ticketId ? null : ticketId;
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
