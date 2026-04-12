import { Component, OnInit } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
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
})
export class Home implements OnInit {
  topRatedProducts: ProductList[] = [];
  bestSellingProducts: ProductList[] = [];
  stats: HomeStats | null = null;
  userName: string = '';
  isLoggedIn: boolean = false;
  isLoading: boolean = true;

  private loadCount = 0;

  constructor(
    private productService: ProductService,
    public auth: AuthService,
    public cartService: CartService,
    public wishlistService: WishlistService
  ) {}

  ngOnInit() {
    this.isLoggedIn = this.auth.isLoggedIn();
    const user = this.auth.getUser();
    if (user) {
      this.userName = user.name;
    }

    this.productService.getHomeStats().subscribe({
      next: stats => this.stats = stats
    });

    this.productService.getTopRatedProducts().subscribe({
      next: products => {
        this.topRatedProducts = products;
        this.checkLoading();
      },
      error: () => this.checkLoading()
    });

    this.productService.getBestSellingProducts().subscribe({
      next: products => {
        this.bestSellingProducts = products;
        this.checkLoading();
      },
      error: () => this.checkLoading()
    });
  }

  private checkLoading() {
    this.loadCount++;
    if (this.loadCount >= 2) {
      this.isLoading = false;
    }
  }

  scrollLeft(container: HTMLElement) {
    container.scrollBy({ left: -300, behavior: 'smooth' });
  }

  scrollRight(container: HTMLElement) {
    container.scrollBy({ left: 300, behavior: 'smooth' });
  }
}
