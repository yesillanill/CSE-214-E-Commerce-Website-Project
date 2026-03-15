import { Injectable, signal } from '@angular/core';
import { ProductList } from '../models/product-list.model';

@Injectable({
  providedIn: 'root',
})
export class WishlistService {
  private wishlistSignal = signal<ProductList[]>([]);

  getWishlist(){
    return this.wishlistSignal();
  }

  add(product: ProductList){
    this.wishlistSignal.update(list => {
      const exists = list.find(p=> p.id===product.id);
      if(!exists){
        return [...list, product];
      }
      return list;
    });
  }

  remove(product: ProductList){
    this.wishlistSignal.update(list => list.filter(p=>p.id!==product.id));
  }

  toggle(product: ProductList){
    this.wishlistSignal.update(list => {
      const exists = list.find(p=>p.id===product.id);
      if(exists){
        return list.filter(p=>p.id!==product.id);
      }else{
        return [...list, product];
      }
    });
  }

  isInWishlist(product: ProductList) : boolean{
    return this.wishlistSignal().some(p=>p.id===product.id);
  }
}
