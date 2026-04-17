import { Component, ChangeDetectionStrategy } from '@angular/core';
import { WishlistService } from '../../../core/services/wishlist.service';
import { CommonModule } from '@angular/common';
import { ProductCard } from '../../shared/product-card/product-card';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-wishlist',
  imports: [CommonModule, ProductCard, TranslateModule],
  templateUrl: './wishlist.html',
  styleUrl: './wishlist.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Wishlist {
  constructor(public wishlist: WishlistService){}
}
