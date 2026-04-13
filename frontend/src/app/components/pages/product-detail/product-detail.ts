import { ProductDetail } from './../../../core/models/product-detail.model';
import { ProductService } from './../../../core/services/product.service';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ReviewService } from '../../../core/services/review.service';
import { Review } from '../../../core/models/review.model';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { SupportService } from '../../../core/services/support.service';
import { ProfanityService } from '../../../core/services/profanity.service';

import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LoadingSpinner } from '../../layout/loading-spinner/loading-spinner';
import { Observable, map, switchMap, catchError, of, tap } from 'rxjs';

@Component({
  selector: 'app-product-detail-page',
  standalone: true,
  imports: [TranslateModule, RouterLink, CommonModule, LoadingSpinner, DecimalPipe, FormsModule],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss',
})
export class ProductDetailPage implements OnInit{
  product$!: Observable<ProductDetail | null>;
  reviews: Review[] = [];
  newRating = 5;
  newComment = '';
  reviewSubmitting = false;
  reviewError = '';
  private currentProductId: number | null = null;

  // Report state
  showReportProduct = false;
  reportProductMessage = '';
  reportProductSubmitting = false;
  reportProductSuccess = false;
  reportProductError = '';
  reportReviewId: number | null = null;
  reportReviewMessage = '';
  reportReviewSubmitting = false;
  reportReviewSuccess = false;
  reportReviewError = '';

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    private reviewService: ReviewService,
    private supportService: SupportService,
    public cart: CartService,
    public wishlist: WishlistService,
    public auth: AuthService,
    private cdr: ChangeDetectorRef,
    private profanity: ProfanityService,
    private translate: TranslateService
  ){}

  ngOnInit(): void {
    this.product$ = this.route.paramMap.pipe(
      map(params => params.get('id')),
      switchMap(id => {
        const numId = Number(id);
        if (!id || isNaN(numId)) {
          return of(null);
        }
        this.currentProductId = numId;
        this.loadReviews(numId);
        return this.productService.getProduct(numId).pipe(
          catchError(() => of(null))
        );
      })
    );
  }

  loadReviews(productId: number): void {
    this.reviewService.getProductReviews(productId).subscribe({
      next: (reviews) => { this.reviews = reviews; this.cdr.markForCheck(); },
      error: () => { this.reviews = []; this.cdr.markForCheck(); }
    });
  }

  submitReview(): void {
    if (!this.currentProductId || this.reviewSubmitting) return;
    if (this.profanity.contains(this.newComment)) {
      this.reviewError = this.translate.instant('ERRORS.PROFANITY');
      return;
    }
    this.reviewSubmitting = true;
    this.reviewError = '';

    this.reviewService.createReview({
      productId: this.currentProductId,
      rating: this.newRating,
      comment: this.newComment
    }).subscribe({
      next: (review) => {
        this.reviews.unshift(review);
        this.newComment = '';
        this.newRating = 5;
        this.reviewSubmitting = false;
        // Refresh product to get updated avg rating
        if (this.currentProductId) {
          this.product$ = this.productService.getProduct(this.currentProductId).pipe(
            catchError(() => of(null))
          );
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.reviewError = err.error?.message || err.error || 'Yorum eklenirken bir hata oluştu.';
        this.reviewSubmitting = false;
        this.cdr.markForCheck();
      }
    });
  }

  addToCart(product: ProductDetail){
    if (product) this.cart.add(product);
  }

  isInWishlist(product: ProductDetail): boolean{
    return product ? this.wishlist.isInWishlist(product) : false;
  }

  toggleWish(product: ProductDetail){
    if (product) this.wishlist.toggle(product);
  }

  // Report product
  toggleReportProduct() {
    this.showReportProduct = !this.showReportProduct;
    this.reportProductMessage = '';
    this.reportProductSuccess = false;
    this.reportProductError = '';
  }

  submitReportProduct() {
    if (!this.currentProductId || !this.reportProductMessage.trim() || this.reportProductSubmitting) return;
    if (this.profanity.contains(this.reportProductMessage)) {
      this.reportProductError = this.translate.instant('ERRORS.PROFANITY');
      return;
    }
    this.reportProductError = '';
    this.reportProductSubmitting = true;
    this.supportService.createTicket({
      subject: 'Product Report',
      message: this.reportProductMessage,
      type: 'PRODUCT',
      productId: this.currentProductId
    }).subscribe({
      next: () => {
        this.reportProductSubmitting = false;
        this.reportProductSuccess = true;
        this.reportProductMessage = '';
        this.cdr.markForCheck();
      },
      error: () => { this.reportProductSubmitting = false; this.cdr.markForCheck(); }
    });
  }

  // Report review
  toggleReportReview(reviewId: number) {
    if (this.reportReviewId === reviewId) {
      this.reportReviewId = null;
    } else {
      this.reportReviewId = reviewId;
      this.reportReviewMessage = '';
      this.reportReviewSuccess = false;
      this.reportReviewError = '';
    }
  }

  submitReportReview(reviewId: number) {
    if (!this.reportReviewMessage.trim() || this.reportReviewSubmitting) return;
    if (this.profanity.contains(this.reportReviewMessage)) {
      this.reportReviewError = this.translate.instant('ERRORS.PROFANITY');
      return;
    }
    this.reportReviewError = '';
    this.reportReviewSubmitting = true;
    this.supportService.createTicket({
      subject: 'Review Report',
      message: this.reportReviewMessage,
      type: 'REVIEW',
      productId: this.currentProductId ?? undefined,
      reviewId: reviewId
    }).subscribe({
      next: () => {
        this.reportReviewSubmitting = false;
        this.reportReviewSuccess = true;
        this.reportReviewMessage = '';
        this.cdr.markForCheck();
      },
      error: () => { this.reportReviewSubmitting = false; this.cdr.markForCheck(); }
    });
  }
}
