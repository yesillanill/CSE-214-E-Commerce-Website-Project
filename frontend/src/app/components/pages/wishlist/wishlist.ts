import { Component } from '@angular/core';
import { WishlistService } from '../../../core/services/wishlist.service';
import { CommonModule } from '@angular/common';
import { ProductCard } from '../../shared/product-card/product-card';

@Component({
  selector: 'app-wishlist',
  imports: [CommonModule, ProductCard],
  templateUrl: './wishlist.html',
  styleUrl: './wishlist.scss',
})
export class Wishlist {
  constructor(public wishlist: WishlistService){}
}
