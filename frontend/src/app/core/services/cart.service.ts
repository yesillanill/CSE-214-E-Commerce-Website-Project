import { Injectable, signal, computed } from '@angular/core';
import { CartItem } from '../models/cart-item.model';
import { ProductList } from '../models/product-list.model';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private apiUrl = 'http://localhost:8080/cart';
  private cartSignal = signal<CartItem[]>([]);

  constructor(private http: HttpClient, private authService: AuthService) {
    this.authService.currentUser$.subscribe(user => {
      if (user) {
        this.loadCart(user.id!);
      } else {
        this.cartSignal.set([]);
      }
    });
  }

  getCart(){
    return this.cartSignal();
  }

  private loadCart(userId: number) {
    this.http.get<CartItem[]>(`${this.apiUrl}/${userId}`).subscribe(items => {
      this.cartSignal.set(items);
    });
  }

  add(product: ProductList){
    const user = this.authService.getUser();
    if(!user || this.authService.isAdmin() || this.authService.isCorporateUser()) return;

    this.http.post<CartItem>(`${this.apiUrl}/add`, { productId: product.id, quantity: 1, userId: user.id })
      .subscribe({
        next: (res) => {
          this.cartSignal.update(cart => {
            const index = cart.findIndex(i=>i.product.id===product.id);
            if(index !== -1){
              const newCart = [...cart];
              newCart[index] = res;
              return newCart;
            }
            return [...cart, res];
          });
        },
        error: (err) => console.error("Error adding to cart:", err)
      });
  }

  increase(item: CartItem){
    if (!item.id) return;
    this.http.patch<CartItem>(`${this.apiUrl}/update/${item.id}?quantity=${item.quantity + 1}`, {})
      .subscribe(res => {
        this.cartSignal.update(cart => {
          const index = cart.findIndex(i=>i.id===item.id);
          if(index !== -1) {
            const newCart = [...cart];
            newCart[index] = res;
            return newCart;
          }
          return cart;
        });
      });
  }

  decrease(item: CartItem){
    if (!item.id) return;
    if (item.quantity <= 1) {
      this.remove(item);
      return;
    }
    
    this.http.patch<CartItem>(`${this.apiUrl}/update/${item.id}?quantity=${item.quantity - 1}`, {})
      .subscribe(res => {
        this.cartSignal.update(cart => {
          const index = cart.findIndex(i=>i.id===item.id);
          if(index !== -1) {
            const newCart = [...cart];
            newCart[index] = res;
            return newCart;
          }
          return cart;
        });
      });
  }

  remove(item: CartItem){
    if (!item.id) return;
    this.http.delete(`${this.apiUrl}/remove/${item.id}`).subscribe(() => {
      this.cartSignal.update(cart => cart.filter(i=>i.id !== item.id));
    });
  }

  getTotal(){
    return this.cartSignal().reduce((total,item) => total + item.product.price * item.quantity, 0);
  }
}
