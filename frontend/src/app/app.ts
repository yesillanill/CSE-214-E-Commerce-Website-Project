import { AppService } from './core/services/app.service';
import { Component, signal } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { Sidebar } from './components/layout/sidebar/sidebar';
import { AuthService } from './core/services/auth.service';
import { CartService } from './core/services/cart.service';
import { WishlistService } from './core/services/wishlist.service';
import { CommonModule, DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, Sidebar, CommonModule, DecimalPipe],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})

export class App {
  constructor(
    private appService: AppService,
    public auth: AuthService,
    public cartService: CartService,
    public wishlistService: WishlistService
  ){
    this.appService.initApp();
  }
}


