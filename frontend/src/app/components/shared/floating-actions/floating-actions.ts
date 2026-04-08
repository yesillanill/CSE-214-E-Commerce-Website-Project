import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router, NavigationEnd } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ChatService, ChatMessage } from '../../../core/services/chat.service';

@Component({
  selector: 'app-floating-actions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './floating-actions.html',
  styleUrl: './floating-actions.scss',
})
export class FloatingActions implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('chatMessagesContainer') chatMessagesContainer!: ElementRef;

  // Chatbot state
  chatMessages: ChatMessage[] = [];
  chatInput = '';
  isChatLoading = false;
  isChatOpen = false;
  private shouldScroll = false;

  // Route
  isAiAssistantPage = false;
  private routerSub!: Subscription;

  constructor(
    public auth: AuthService,
    public cartService: CartService,
    public wishlistService: WishlistService,
    private chatService: ChatService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.checkRoute(this.router.url);

    this.routerSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e) => {
        const nav = e as NavigationEnd;
        this.checkRoute(nav.urlAfterRedirects);
        // Close panel on route change
        if (this.isChatOpen) {
          this.isChatOpen = false;
        }
      });

    this.initChat();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll && this.chatMessagesContainer) {
      this.chatMessagesContainer.nativeElement.scrollTop =
        this.chatMessagesContainer.nativeElement.scrollHeight;
      this.shouldScroll = false;
    }
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
  }

  private checkRoute(url: string): void {
    this.isAiAssistantPage = url === '/ai-assistant';
  }

  private initChat(): void {
    const isLoggedIn = this.auth.isLoggedIn();
    const userName = this.auth.getUser()?.name || '';

    const greeting = isLoggedIn
      ? `Merhaba ${userName}! 🤖 Size nasıl yardımcı olabilirim?`
      : `Merhaba! 🤖 Size ürünler, kategoriler ve mağazalar hakkında yardımcı olabilirim. Kişisel bilgiler için giriş yapmanız gerekir.`;

    this.chatMessages.push({
      role: 'assistant',
      content: greeting,
      timestamp: new Date()
    });
  }

  toggleChat(): void {
    this.isChatOpen = !this.isChatOpen;
    if (this.isChatOpen) {
      this.shouldScroll = true;
    }
  }

  sendChatMessage(): void {
    if (!this.chatInput.trim() || this.isChatLoading) return;
    const question = this.chatInput.trim();
    this.chatInput = '';

    this.chatMessages.push({ role: 'user', content: question, timestamp: new Date() });
    this.isChatLoading = true;
    this.shouldScroll = true;

    const role = this.auth.getRole() as string || 'GUEST';
    const mappedRole = role === 'Admin' ? 'ADMIN'
                     : role === 'CorporateUser' ? 'CORPORATE'
                     : role === 'IndividualUser' ? 'INDIVIDUAL'
                     : 'GUEST';
    const userId = this.auth.getUser()?.id;

    this.chatService.askQuestion(question, mappedRole, userId).subscribe({
      next: (res) => {
        this.chatMessages.push({ role: 'assistant', content: res.answer, timestamp: new Date() });
        this.isChatLoading = false;
        this.shouldScroll = true;
      },
      error: () => {
        this.chatMessages.push({ role: 'assistant', content: 'Üzgünüm, bir hata oluştu. Lütfen tekrar deneyin.', timestamp: new Date() });
        this.isChatLoading = false;
        this.shouldScroll = true;
      }
    });
  }

  onChatKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendChatMessage();
    }
  }
}
