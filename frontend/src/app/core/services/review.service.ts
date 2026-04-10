import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Review, ReviewCreate } from '../models/review.model';

@Injectable({
  providedIn: 'root',
})
export class ReviewService {
  private api = 'http://localhost:8080/api/reviews';

  constructor(private http: HttpClient) {}

  getProductReviews(productId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${this.api}/product/${productId}`);
  }

  getUserReviews(): Observable<Review[]> {
    return this.http.get<Review[]>(`${this.api}/user`);
  }

  createReview(review: ReviewCreate): Observable<Review> {
    return this.http.post<Review>(this.api, review);
  }

  deleteReview(reviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${reviewId}`);
  }
}
