import { Injectable, signal, computed } from '@angular/core';
import { CartItem } from '../models/cart-item.model';
import { ProductList } from '../models/product-list.model';

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private cartSignal = signal<CartItem[]>([]);

  getCart(){
    return this.cartSignal();
  }

  add(product: ProductList){
    this.cartSignal.update(cart => {
      const index = cart.findIndex(i=>i.product.id===product.id);
      if(index !== -1){
        const newCart = [...cart];
        newCart[index] = {...newCart[index], quantity: newCart[index].quantity + 1};
        return newCart;
      }
      return [...cart, {product,quantity:1}];
    });
  }

  increase(item: CartItem){
    this.cartSignal.update(cart => {
      const index = cart.findIndex(i=>i.product.id===item.product.id);
      if(index !== -1) {
        const newCart = [...cart];
        newCart[index] = {...newCart[index], quantity: newCart[index].quantity + 1};
        return newCart;
      }
      return cart;
    });
  }

  decrease(item: CartItem){
    this.cartSignal.update(cart => {
      const index = cart.findIndex(i=>i.product.id===item.product.id);
      if(index !== -1 && cart[index].quantity > 1) {
        const newCart = [...cart];
        newCart[index] = {...newCart[index], quantity: newCart[index].quantity - 1};
        return newCart;
      }
      return cart;
    });
  }

  remove(item: CartItem){
    this.cartSignal.update(cart => cart.filter(i=>i.product.id!==item.product.id));
  }

  getTotal(){
    return this.cartSignal().reduce((total,item) => total + item.product.price * item.quantity, 0);
  }
}
