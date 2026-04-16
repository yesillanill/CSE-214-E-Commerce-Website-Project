import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router, NavigationEnd } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ChatService, ChatMessage } from '../../../core/services/chat.service';
import { TranslateModule } from '@ngx-translate/core';
import * as Plotly from 'plotly.js-dist-min';
import { ChangeDetectorRef } from '@angular/core';
import { ThemeService } from '../../../core/services/theme.service';
import DOMPurify from 'dompurify';

@Component({
  selector: 'app-floating-actions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TranslateModule],
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
  private authUserSub!: Subscription;
  private themeSub!: Subscription;

  constructor(
    public auth: AuthService,
    public cartService: CartService,
    public wishlistService: WishlistService,
    private chatService: ChatService,
    private themeService: ThemeService,
    private router: Router,
    private cdr: ChangeDetectorRef
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

    this.authUserSub = this.auth.currentUser$.subscribe((user) => {
      this.resetChat(!!user, user?.name);
    });

    this.themeSub = this.themeService.theme$.subscribe((theme: string) => {
      const isDark = theme === 'dark';
      const fontColor = isDark ? '#ffffff' : '#2d3748';
      this.updateAllChartsTheme(fontColor);
    });
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
    this.authUserSub?.unsubscribe();
    this.themeSub?.unsubscribe();
  }

  private updateAllChartsTheme(fontColor: string): void {
    this.chatMessages.forEach(msg => {
      if (msg.chart && msg.id) {
        const chartElementId = 'chart-' + msg.id;
        const el = document.getElementById(chartElementId);
        if (el && (el as any).data) { // Ensure plot is rendered before relayout
          try {
            Plotly.relayout(chartElementId, { 'font.color': fontColor } as any);
          } catch (e) {
            console.error("Plotly relayout error:", e);
          }
        }
      }
    });
  }

  private checkRoute(url: string): void {
    this.isAiAssistantPage = url === '/ai-assistant';
  }

  private resetChat(isLoggedIn: boolean, userName?: string): void {
    this.chatMessages = [];
    const greeting = isLoggedIn
      ? `Merhaba ${userName}! 🤖 Size nasıl yardımcı olabilirim?`
      : `Merhaba! 🤖 Size ürünler, kategoriler ve mağazalar hakkında yardımcı olabilirim. Siparişlerinizi takip etmek için giriş yapmanız gerekir.`;

    this.chatMessages.push({
      id: 'msg-' + Date.now().toString(),
      role: 'assistant',
      content: greeting,
      timestamp: new Date()
    });
    // Use detectChanges safely since this is floating actions
    try {
      this.cdr.detectChanges();
    } catch(e) {}
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

    const userMsgId = 'msg-' + Date.now().toString() + '-u';
    this.chatMessages.push({ id: userMsgId, role: 'user', content: question, timestamp: new Date() });
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
        const assistantMsgId = 'msg-' + Date.now().toString() + '-a';
        const msg: ChatMessage = { id: assistantMsgId, role: 'assistant', content: res.answer, timestamp: new Date() };
        if (res.chart) {
            msg.chart = res.chart;
        }
        this.chatMessages.push(msg);
        this.isChatLoading = false;
        this.shouldScroll = true;
        this.cdr.detectChanges();
        
        if (res.chart) {
            const chartDataToRender = JSON.parse(res.chart!);
            const chartElementId = 'chart-' + assistantMsgId;
            let retryCount = 0;
            const maxRetries = 10;
            
            const attemptRender = () => {
                const el = document.getElementById(chartElementId);
                if (el) {
                    try {
                        const layout = chartDataToRender.layout || {};
                        layout.autosize = true;
                        // Increase bottom margin to make room for legend
                        layout.margin = { l: 20, r: 20, t: 40, b: 80 };
                        layout.paper_bgcolor = 'transparent';
                        const isDark = document.body.classList.contains('dark');
                        layout.font = { color: isDark ? '#ffffff' : '#2d3748', size: 11 };
                        
                        // Move legend to the bottom horizontally to prevent overlapping
                        layout.legend = {
                            orientation: 'h',
                            yanchor: 'top',
                            y: -0.1,
                            xanchor: 'center',
                            x: 0.5
                        };
                        
                        Plotly.newPlot(chartElementId, chartDataToRender.data, layout, { responsive: true, displayModeBar: false })
                          .then(() => {
                              // Ensure layout resize is triggered after plot
                              setTimeout(() => {
                                  Plotly.Plots.resize(el as any);
                              }, 50);
                          });
                    } catch (e) {
                         console.error("Plotly rendering error:", e);
                    }
                } else if (retryCount < maxRetries) {
                    retryCount++;
                    setTimeout(attemptRender, 100);
                } else {
                    console.error('Chart element not found in DOM after retries: ', chartElementId);
                }
            };
            setTimeout(attemptRender, 50);
        }
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

  renderMarkdown(text: string): string {
    if (!text) return '';

    let html = text;
    // Escape HTML entities first
    html = html.replace(/&/g, '&amp;')
               .replace(/</g, '&lt;')
               .replace(/>/g, '&gt;');

    // Convert markdown links
    html = html.replace(/\[([^\]]+)\]\((https?:\/\/localhost:4200\/[^)]+)\)/g,
      '<a class="chat-link" href="$2" data-internal="true">$1</a>');
    html = html.replace(/\[([^\]]+)\]\((https?:\/\/[^)]+)\)/g,
      '<a class="chat-link external" href="$2" target="_blank" rel="noopener">$1</a>');

    // Bold & Italic
    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');

    // Bullet points
    html = html.replace(/^[\-\*]\s+(.+)$/gm, '<li>$1</li>');
    html = html.replace(/((?:<li>.*<\/li>\n?)+)/g, '<ul>$1</ul>');

    // Newlines
    html = html.replace(/\n/g, '<br>');
    html = html.replace(/<\/li><br><li>/g, '</li><li>');

    // Make sure DOMPurify allows target="_blank" and our custom class/data attributes
    return DOMPurify.sanitize(html, {
        ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'p', 'br', 'ul', 'li', 'span', 'div'],
        ALLOWED_ATTR: ['href', 'target', 'rel', 'class', 'data-internal']
    });
  }

  onMessageClick(event: Event) {
    const target = event.target as HTMLElement;
    if (target.tagName === 'A' && target.getAttribute('data-internal') === 'true') {
      event.preventDefault();
      const href = target.getAttribute('href');
      if (href) {
        try {
          const url = new URL(href);
          this.router.navigateByUrl(url.pathname);
        } catch (e) {
          this.router.navigateByUrl(href);
        }
      }
    }
  }
}

