import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ReviewService } from '../../../core/services/review.service';
import { Review } from '../../../core/models/review.model';
import { LoadingSpinner } from '../../layout/loading-spinner/loading-spinner';

@Component({
  selector: 'app-my-reviews',
  standalone: true,
  imports: [RouterLink, TranslateModule, DatePipe, LoadingSpinner],
  templateUrl: './my-reviews.html',
  styleUrl: './my-reviews.scss',
})
export class MyReviews implements OnInit {
  reviews: Review[] = [];
  isLoading = true;

  constructor(private reviewService: ReviewService) {}

  ngOnInit() {
    this.loadReviews();
  }

  loadReviews() {
    this.isLoading = true;
    this.reviewService.getUserReviews().subscribe({
      next: (reviews) => {
        this.reviews = reviews;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  deleteReview(reviewId: number) {
    this.reviewService.deleteReview(reviewId).subscribe({
      next: () => {
        this.reviews = this.reviews.filter((r) => r.id !== reviewId);
      },
    });
  }

  getStars(rating: number): string {
    return '★'.repeat(rating) + '☆'.repeat(5 - rating);
  }
}
