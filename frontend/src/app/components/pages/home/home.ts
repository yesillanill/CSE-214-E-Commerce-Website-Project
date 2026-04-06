import { Component, OnInit, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ProductList } from '../../../core/models/product-list.model';
import { ProductCard } from '../../shared/product-card/product-card';
import { LoadingSpinner } from '../../layout/loading-spinner/loading-spinner';

interface HomeStats {
  categoryCount: number;
  brandCount: number;
  storeCount: number;
  productCount: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCard, LoadingSpinner],
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

    this.productService.getHomeStats().subscribe(stats => {
      this.stats = stats;
    });

    this.productService.getTopRatedProducts().subscribe(products => {
      this.topRatedProducts = products;
      this.checkLoading();
    });

    this.productService.getBestSellingProducts().subscribe(products => {
      this.bestSellingProducts = products;
      this.checkLoading();
    });
  }

  private checkLoading() {
    if (this.topRatedProducts.length > 0 || this.bestSellingProducts.length > 0) {
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
