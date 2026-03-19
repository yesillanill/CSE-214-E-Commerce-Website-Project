import { Component } from '@angular/core';
import { CartService } from '../../../core/services/cart.service';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-cart',
  imports: [CommonModule, TranslateModule, RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.scss',
})

export class Cart {
  constructor(public cart: CartService){}
}
