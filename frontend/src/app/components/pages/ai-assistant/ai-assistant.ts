import { Component, ViewChild, ElementRef, AfterViewChecked, ChangeDetectorRef, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ChatService, ChatMessage } from '../../../core/services/chat.service';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService, ThemeMode } from '../../../core/services/theme.service';
import { Router } from '@angular/router';
import * as Plotly from 'plotly.js-dist-min';
import DOMPurify from 'dompurify';
import { Subscription } from 'rxjs';
import { containsSqlInjection, SQL_BLOCKED_MESSAGE } from '../../../core/utils/sql-guard';

@Component({
  selector: 'app-ai-asistant',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './ai-assistant.html',
  styleUrl: './ai-assistant.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiAssistant implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('chatContainer') chatContainer!: ElementRef;

  messages: ChatMessage[] = [];
  userInput = '';
  isLoading = false;
  private shouldScroll = false;

  private authSub!: Subscription;
  private themeSub!: Subscription;

  constructor(
    private chatService: ChatService,
    public auth: AuthService,
    private themeService: ThemeService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
  }

  ngOnInit() {
    this.authSub = this.auth.currentUser$.subscribe((user) => {
      this.resetChat(!!user, user?.name);
    });

    this.themeSub = this.themeService.theme$.subscribe((theme: string) => {
      const isDark = theme === 'dark';
      const fontColor = isDark ? '#ffffff' : '#2d3748';
      this.updateAllChartsTheme(fontColor);
    });
  }

  ngOnDestroy() {
    this.authSub?.unsubscribe();
    this.themeSub?.unsubscribe();
  }

  private updateAllChartsTheme(fontColor: string) {
    this.messages.forEach(msg => {
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

  private resetChat(isLoggedIn: boolean, userName?: string) {
    this.messages = [];
    const greeting = isLoggedIn
      ? `Merhaba ${userName}! 👋 Ben yapay zeka asistanınızım. Ürünler, kategoriler, mağazalar veya kişisel bilgileriniz hakkında sorular sorabilirsiniz.`
      : `Merhaba! 👋 Ben yapay zeka asistanınızım. Ürünler, kategoriler ve mağazalar hakkında sorular sorabilirsiniz. Siparişlerinizi takip etmek ve detaylı bilgiler için lütfen giriş yapın.`;

    this.messages.push({
      role: 'assistant',
      content: greeting,
      timestamp: new Date()
    });
    this.cdr.markForCheck();
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

    this.shouldScroll = true;

    // Client-side SQL injection check
    if (containsSqlInjection(question)) {
      this.messages.push({
        role: 'assistant',
        content: SQL_BLOCKED_MESSAGE,
        timestamp: new Date()
      });
      this.cdr.markForCheck();
      return;
    }

    this.isLoading = true;

    const role = this.auth.getRole() as string || 'GUEST';
    const mappedRole = role === 'Admin' ? 'ADMIN'
                     : role === 'CorporateUser' ? 'CORPORATE'
                     : role === 'IndividualUser' ? 'INDIVIDUAL'
                     : 'GUEST';

    const userId = this.auth.getUser()?.id;

    this.chatService.askQuestion(question, mappedRole, userId).subscribe({
      next: (res) => {
        const assistantMsgId = 'msg-' + Date.now().toString() + '-a';
        const msg: ChatMessage = {
          id: assistantMsgId,
          role: 'assistant',
          content: res.answer,
          timestamp: new Date()
        };
        if (res.chart) {
            msg.chart = res.chart;
        }
        
        this.messages.push(msg);
        this.isLoading = false;
        this.shouldScroll = true;
        this.cdr.markForCheck();
        
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
      error: (err) => {
        this.messages.push({
          role: 'assistant',
          content: 'Üzgünüm, bir hata oluştu. Lütfen tekrar deneyin.',
          timestamp: new Date()
        });
        this.isLoading = false;
        this.shouldScroll = true;
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Convert markdown-style content to safe HTML with clickable links.
   * Supports: [text](url), **bold**, *italic*, bullet lists
   */
  renderMarkdown(text: string): string {
    if (!text) return '';

    let html = text;

    // Escape HTML entities first
    html = html.replace(/&/g, '&amp;')
               .replace(/</g, '&lt;')
               .replace(/>/g, '&gt;');

    // Convert markdown links [text](url) to <a> tags
    // Handle internal links (localhost:4200) as router links
    html = html.replace(/\[([^\]]+)\]\((https?:\/\/localhost:4200\/[^)]+)\)/g,
      '<a class="chat-link" href="$2" data-internal="true">$1</a>');

    // Handle external links
    html = html.replace(/\[([^\]]+)\]\((https?:\/\/[^)]+)\)/g,
      '<a class="chat-link external" href="$2" target="_blank" rel="noopener">$1</a>');

    // Convert **bold**
    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

    // Convert *italic*
    html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');

    // Convert bullet points (lines starting with - or *)
    html = html.replace(/^[\-\*]\s+(.+)$/gm, '<li>$1</li>');
    // Wrap consecutive <li> items in <ul>
    html = html.replace(/((?:<li>.*<\/li>\n?)+)/g, '<ul>$1</ul>');

    // Convert newlines to <br>
    html = html.replace(/\n/g, '<br>');

    // Clean up <br> inside <ul> and after <ul>
    html = html.replace(/<br><ul>/g, '<ul>');
    html = html.replace(/<\/li><br><li>/g, '</li><li>');

    // Make sure DOMPurify allows target="_blank" and our custom class/data attributes
    return DOMPurify.sanitize(html, {
        ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'p', 'br', 'ul', 'li', 'span', 'div'],
        ALLOWED_ATTR: ['href', 'target', 'rel', 'class', 'data-internal']
    });
  }

  /**
   * Handle click events on chat messages to intercept internal links
   */
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
          // Fallback: try using href directly as path
          this.router.navigateByUrl(href);
        }
      }
    }
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
