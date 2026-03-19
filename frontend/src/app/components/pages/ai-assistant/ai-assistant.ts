import { Component, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ChatService, ChatMessage } from '../../../core/services/chat.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-ai-asistant',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './ai-assistant.html',
  styleUrl: './ai-assistant.scss',
})
export class AiAssistant implements AfterViewChecked {
  @ViewChild('chatContainer') chatContainer!: ElementRef;

  messages: ChatMessage[] = [];
  userInput = '';
  isLoading = false;
  private shouldScroll = false;

  constructor(
    private chatService: ChatService,
    public auth: AuthService
  ) {
    this.messages.push({
      role: 'assistant',
      content: 'Hello! I am your AI assistant. How can I help you today?',
      timestamp: new Date()
    });
  }

  ngAfterViewChecked() {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  sendMessage() {
    if (!this.userInput.trim() || this.isLoading) return;

    const question = this.userInput.trim();
    this.userInput = '';

    this.messages.push({
      role: 'user',
      content: question,
      timestamp: new Date()
    });

    this.isLoading = true;
    this.shouldScroll = true;

    const role = this.auth.getRole() as string || 'INDIVIDUAL';
    const mappedRole = role === 'Admin' ? 'ADMIN'
                     : role === 'CorporateUser' ? 'CORPORATE'
                     : 'INDIVIDUAL';

    this.chatService.askQuestion(question, mappedRole).subscribe({
      next: (res) => {
        this.messages.push({
          role: 'assistant',
          content: res.answer,
          timestamp: new Date()
        });
        this.isLoading = false;
        this.shouldScroll = true;
      },
      error: (err) => {
        this.messages.push({
          role: 'assistant',
          content: 'Sorry, something went wrong. Please try again.',
          timestamp: new Date()
        });
        this.isLoading = false;
        this.shouldScroll = true;
      }
    });
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom() {
    try {
      this.chatContainer.nativeElement.scrollTop = this.chatContainer.nativeElement.scrollHeight;
    } catch (e) {}
  }
}
