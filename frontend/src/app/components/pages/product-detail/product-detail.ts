import { ProductDetail } from './../../../core/models/product-detail.model';
import { ProductService } from './../../../core/services/product.service';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';

import { CommonModule, DecimalPipe } from '@angular/common';
import { LoadingSpinner } from '../../layout/loading-spinner/loading-spinner';
import { Observable, map, switchMap, catchError, of, tap } from 'rxjs';

@Component({
  selector: 'app-product-detail-page',
  standalone: true,
  imports: [TranslateModule, RouterLink, CommonModule, LoadingSpinner, DecimalPipe],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss',
})
export class ProductDetailPage implements OnInit{
  product$!: Observable<ProductDetail | null>;

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    public cart: CartService,
    public wishlist: WishlistService,
    public auth: AuthService
  ){}

  ngOnInit(): void {
    console.log('ProductDetailPage initialized');
    this.product$ = this.route.paramMap.pipe(
      map(params => params.get('id')),
      tap(id => console.log('Product ID from route:', id)),
      switchMap(id => {
        const numId = Number(id);
        if (!id || isNaN(numId)) {
          console.warn('Invalid ID:', id);
          return of(null);
        }
        return this.productService.getProduct(numId).pipe(
          tap(p => console.log('Product loaded successfully:', p)),
          catchError(err => {
            console.error('Error in getProduct API:', err);
            return of(null);
          })
        );
      })
    );
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
}
