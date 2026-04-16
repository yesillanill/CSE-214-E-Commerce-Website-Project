import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatMessage {
  id?: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  chart?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ChatService {
  private apiUrl = 'http://localhost:8080/api/chat/ask';

  constructor(private http: HttpClient) {}

  askQuestion(question: string, role: string, userId?: number): Observable<{ answer: string, chart?: string }> {
    const body: any = { question, role };
    if (userId) {
      body.userId = userId;
    }
    return this.http.post<{ answer: string, chart?: string }>(this.apiUrl, body);
  }
}
