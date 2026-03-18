import { Component, Input } from '@angular/core';
import { ProductList } from '../../../core/models/product-list.model';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-product-card',
  imports: [RouterLink, TranslateModule],
  templateUrl: './product-card.html',
  styleUrl: './product-card.scss',
})
export class ProductCard {
  @Input() product!: ProductList;
  constructor(public cart: CartService, public wishlist: WishlistService, public auth: AuthService){}

  addToCart(){
    this.cart.add(this.product);
  }

  isInWishlist(): boolean{
    return this.wishlist.isInWishlist(this.product);
  }

  toggleWish(){
    this.wishlist.toggle(this.product);
  }

}
