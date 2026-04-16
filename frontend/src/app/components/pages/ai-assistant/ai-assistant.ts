import { Component, ViewChild, ElementRef, AfterViewChecked, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ChatService, ChatMessage } from '../../../core/services/chat.service';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';
import * as Plotly from 'plotly.js-dist-min';

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
    public auth: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    const isLoggedIn = this.auth.isLoggedIn();
    const userName = this.auth.getUser()?.name || '';

    let greeting = isLoggedIn
      ? `Merhaba ${userName}! 👋 Ben yapay zeka asistanınızım. Ürünler, kategoriler, mağazalar veya kişisel bilgileriniz hakkında sorular sorabilirsiniz.`
      : `Merhaba! 👋 Ben yapay zeka asistanınızım. Ürünler, kategoriler ve mağazalar hakkında sorular sorabilirsiniz. Kişisel bilgileriniz için giriş yapmanız gerekir.`;

    this.messages.push({
      role: 'assistant',
      content: greeting,
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
            setTimeout(() => {
                try {
                    const chartElementId = 'chart-' + assistantMsgId;
                    const chartData = JSON.parse(res.chart!);
                    const layout = chartData.layout || {};
                    
                    // Adjust layout to fit container and dark schema naturally
                    layout.autosize = true;
                    layout.margin = { l: 20, r: 20, t: 40, b: 20 };
                    layout.paper_bgcolor = 'transparent';
                    layout.plot_bgcolor = 'transparent';
                    layout.font = { color: '#ffffff' };
                    
                    Plotly.newPlot(chartElementId, chartData.data, layout, { responsive: true, displayModeBar: false });
                } catch (e) {
                    console.error("Plotly rendering error:", e);
                }
            }, 100);
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
    html = html.replace(/<\/ul><br>/g, '</ul>');
    html = html.replace(/<\/li><br><li>/g, '</li><li>');

    return html;
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
