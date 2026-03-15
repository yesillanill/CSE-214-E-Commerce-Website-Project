import { Injectable } from '@angular/core';
import { ProductList } from '../models/product-list.model';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProductDetail } from '../models/product-detail.model';

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private api = "http://localhost:8080/products";

  constructor(private http: HttpClient){}

  getProducts(): Observable<ProductList[]>{
    return this.http.get<ProductList[]>(this.api);
  }

  getProduct(id: number): Observable<ProductDetail>{
    return this.http.get<ProductDetail>(`${this.api}/${id}`);
  }

  getProductsByBrand(name: string): Observable<ProductList[]>{
    return this.http.get<ProductList[]>(`${this.api}/brand/${name}`)
  }

  getProductsByCategory(name: string): Observable<ProductList[]>{
    return this.http.get<ProductList[]>(`${this.api}/category/${name}`)
  }

  getProductsByStore(name: string): Observable<ProductList[]>{
    return this.http.get<ProductList[]>(`${this.api}/store/${name}`)
  }

  addProduct(product: ProductList){
    this.http.post(this.api, product);
  }

  updateProduct(product: ProductList){
    this.http.put(`${this.api}/${product.id}`,product);
  }

  deleteProduct(id: number){
    this.http.delete(`${this.api}/${id}`);
  }

  getCategories(): Observable<string[]>{
    return this.http.get<string[]>(`${this.api}/categories`);
  }
}

