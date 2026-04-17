import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CartService } from '../../../core/services/cart.service';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-cart',
  imports: [CommonModule, TranslateModule, RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})

export class Cart {
  constructor(
    public cart: CartService,
    private router: Router
  ) {}

  checkout() {
    this.router.navigate(['/checkout']);
  }
}
