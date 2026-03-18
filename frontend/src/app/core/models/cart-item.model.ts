import { ProductList } from "./product-list.model"

export interface CartItem{
  id?: number;
  product: ProductList;
  quantity: number;
}
