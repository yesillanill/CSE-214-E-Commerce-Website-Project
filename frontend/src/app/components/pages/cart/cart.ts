import { Component } from '@angular/core';
import { CartService } from '../../../core/services/cart.service';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-cart',
  imports: [CommonModule, TranslateModule, RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.scss',
})

export class Cart {
  constructor(
    public cart: CartService,
    private http: HttpClient,
    private auth: AuthService,
    private router: Router
  ) {}

  checkout() {
    const user = this.auth.getUser();
    if (!user) return;

    this.http.post<any>('http://localhost:8080/api/orders/checkout', {
      userId: user.id,
      shippingAddress: 'Default Address'
    }).subscribe({
      next: () => {
        this.cart.refreshCart();
        this.router.navigate(['/orders']);
      },
      error: (err) => console.error('Checkout failed:', err)
    });
  }
}
