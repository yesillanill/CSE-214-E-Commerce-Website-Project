import { Injectable, signal } from '@angular/core';
import { ProductList } from '../models/product-list.model';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { WishlistItem } from '../models/whislist.model';

@Injectable({
  providedIn: 'root',
})
export class WishlistService {
  private apiUrl = 'http://localhost:8080/wishlist';
  private wishlistSignal = signal<WishlistItem[]>([]);

  constructor(private http: HttpClient, private authService: AuthService) {
    this.authService.currentUser$.subscribe(user => {
      if (user) {
        this.loadWishlist(user.id!);
      } else {
        this.wishlistSignal.set([]);
      }
    });
  }

  private loadWishlist(userId: number) {
    this.http.get<WishlistItem[]>(`${this.apiUrl}/${userId}`).subscribe({
      next: (items) => {
        this.wishlistSignal.set(items);
        console.log("Wishlist loaded for user", userId, items);
      },
      error: (err) => {
        console.error("Wishlist API failed to load items for user", userId, err);
      }
    });
  }

  getWishlist(){
    // Return ProductList[] for backward compatibility with UI if it expects ProductList
    // But actually we have WishlistItem now. Let's return ProductList[]
    return this.wishlistSignal().map(w => w.product);
  }

  add(product: ProductList){
    const user = this.authService.getUser();
    if(!user || this.authService.isAdmin() || this.authService.isCorporateUser()) return;

    this.http.post<WishlistItem>(`${this.apiUrl}/add`, { productId: product.id, userId: user.id })
      .subscribe({
        next: (res) => {
          this.wishlistSignal.update(list => {
            const exists = list.find(w => w.product.id === product.id);
            if(!exists){
              return [...list, res];
            }
            return list;
          });
        },
        error: (err) => console.error("Error adding to wishlist:", err)
      });
  }

  remove(product: ProductList){
    const item = this.wishlistSignal().find(w => w.product.id === product.id);
    if (!item || !item.id) return;

    this.http.delete(`${this.apiUrl}/remove/${item.id}`).subscribe(() => {
      this.wishlistSignal.update(list => list.filter(w => w.id !== item.id));
    });
  }

  toggle(product: ProductList){
    const exists = this.wishlistSignal().find(w => w.product.id === product.id);
    if(exists){
      this.remove(product);
    } else {
      this.add(product);
    }
  }

  isInWishlist(product: ProductList) : boolean{
    return this.wishlistSignal().some(w => w.product.id === product.id);
  }
}
