import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PublicStats,
  IndividualAnalytics,
  CorporateAnalytics,
  AdminAnalytics
} from '../models/analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  getPublicStats(): Observable<PublicStats> {
    return this.http.get<PublicStats>(`${this.baseUrl}/api/public/stats`);
  }

  getIndividualAnalytics(): Observable<IndividualAnalytics> {
    return this.http.get<IndividualAnalytics>(`${this.baseUrl}/api/analytics/individual`);
  }

  getCorporateAnalytics(): Observable<CorporateAnalytics> {
    return this.http.get<CorporateAnalytics>(`${this.baseUrl}/api/analytics/corporate`);
  }

  getAdminAnalytics(): Observable<AdminAnalytics> {
    return this.http.get<AdminAnalytics>(`${this.baseUrl}/api/analytics/admin`);
  }
}
