import { Component, OnInit, OnDestroy, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { ProductService } from '../../../core/services/product.service';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ProductList } from '../../../core/models/product-list.model';
import { ProductCard } from '../../shared/product-card/product-card';
import { LoadingSpinner } from '../../layout/loading-spinner/loading-spinner';
import { TranslateModule } from '@ngx-translate/core';

interface HomeStats {
  categoryCount: number;
  brandCount: number;
  storeCount: number;
  productCount: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, ProductCard, LoadingSpinner, TranslateModule, LowerCasePipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Home implements OnInit, OnDestroy {
  topRatedProducts: ProductList[] = [];
  bestSellingProducts: ProductList[] = [];
  stats: HomeStats | null = null;
  userName: string = '';
  isLoggedIn: boolean = false;
  isLoadingTopRated: boolean = true;
  isLoadingBestSelling: boolean = true;

  private authSub!: Subscription;

  constructor(
    private productService: ProductService,
    public auth: AuthService,
    public cartService: CartService,
    public wishlistService: WishlistService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    // Subscribe to auth state changes so hero section updates on login/logout
    this.authSub = this.auth.currentUser$.subscribe(user => {
      this.isLoggedIn = user !== null && this.auth.getToken() !== null;
      this.userName = user?.name ?? '';
      this.cdr.markForCheck();
    });

    this.productService.getHomeStats().subscribe({
      next: stats => { this.stats = stats; this.cdr.markForCheck(); }
    });

    this.productService.getTopRatedProducts().subscribe({
      next: products => {
        this.topRatedProducts = products;
        this.isLoadingTopRated = false;
        this.cdr.markForCheck();
      },
      error: () => { this.isLoadingTopRated = false; this.cdr.markForCheck(); }
    });

    this.productService.getBestSellingProducts().subscribe({
      next: products => {
        this.bestSellingProducts = products;
        this.isLoadingBestSelling = false;
        this.cdr.markForCheck();
      },
      error: () => { this.isLoadingBestSelling = false; this.cdr.markForCheck(); }
    });
  }

  ngOnDestroy() {
    this.authSub?.unsubscribe();
  }

  scrollLeft(container: HTMLElement) {
    container.scrollBy({ left: -300, behavior: 'smooth' });
  }

  scrollRight(container: HTMLElement) {
    container.scrollBy({ left: 300, behavior: 'smooth' });
  }
}

