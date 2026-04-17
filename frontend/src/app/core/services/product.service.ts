import { Injectable } from '@angular/core';
import { ProductList } from '../models/product-list.model';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { ProductDetail } from '../models/product-detail.model';
import { environment } from '../../../environments/environment';

interface HomeStats {
  categoryCount: number;
  brandCount: number;
  storeCount: number;
  productCount: number;
}

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private api = `${environment.apiUrl}/products`;

  // Cached observables
  private topRatedCache$?: Observable<ProductList[]>;
  private bestSellingCache$?: Observable<ProductList[]>;
  private homeStatsCache$?: Observable<HomeStats>;
  private categoriesCache$?: Observable<string[]>;
  private allProductsCache$?: Observable<ProductList[]>;

  constructor(private http: HttpClient){}

  getProducts(): Observable<ProductList[]>{
    if (!this.allProductsCache$) {
      this.allProductsCache$ = this.http.get<ProductList[]>(this.api).pipe(shareReplay(1));
    }
    return this.allProductsCache$;
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
    if (!this.categoriesCache$) {
      this.categoriesCache$ = this.http.get<string[]>(`${this.api}/categories`).pipe(shareReplay(1));
    }
    return this.categoriesCache$;
  }

  getTopRatedProducts(): Observable<ProductList[]>{
    if (!this.topRatedCache$) {
      this.topRatedCache$ = this.http.get<ProductList[]>(`${this.api}/top-rated`).pipe(shareReplay(1));
    }
    return this.topRatedCache$;
  }

  getBestSellingProducts(): Observable<ProductList[]>{
    if (!this.bestSellingCache$) {
      this.bestSellingCache$ = this.http.get<ProductList[]>(`${this.api}/best-selling`).pipe(shareReplay(1));
    }
    return this.bestSellingCache$;
  }

  getHomeStats(): Observable<HomeStats>{
    if (!this.homeStatsCache$) {
      this.homeStatsCache$ = this.http.get<HomeStats>(`${this.api}/home-stats`).pipe(shareReplay(1));
    }
    return this.homeStatsCache$;
  }

  /** Preload home page data so it's ready when user navigates */
  preloadHomeData(): void {
    this.getHomeStats().subscribe();
    this.getTopRatedProducts().subscribe();
    this.getBestSellingProducts().subscribe();
    this.getCategories().subscribe();
  }

  /** Clear all caches (call after product add/edit/delete) */
  invalidateCache(): void {
    this.topRatedCache$ = undefined;
    this.bestSellingCache$ = undefined;
    this.homeStatsCache$ = undefined;
    this.categoriesCache$ = undefined;
    this.allProductsCache$ = undefined;
  }
}
