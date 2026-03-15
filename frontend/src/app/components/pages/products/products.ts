import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { ProductList } from '../../../core/models/product-list.model';
import { BehaviorSubject, combineLatest, map, Observable, switchMap, tap, catchError, of } from 'rxjs';
import { ProductCard } from '../../shared/product-card/product-card';
import { CommonModule, DecimalPipe } from '@angular/common';
import { LoadingSpinner } from '../../layout/loading-spinner/loading-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';

@Component({
  selector: 'app-products',
  templateUrl: './products.html',
  styleUrl: './products.scss',
  standalone: true,
  imports: [ProductCard, CommonModule, LoadingSpinner, TranslateModule, RouterLink, DecimalPipe]
})
export class Products implements OnInit {
  products$!: Observable<ProductList[]>;
  filteredProducts$!: Observable<ProductList[]>;
  categories$!: Observable<string[]>;
  
  // Filter States
  searchQuery$ = new BehaviorSubject<string>('');
  selectedCategory$ = new BehaviorSubject<string>('all');
  sortOption$ = new BehaviorSubject<string>('default');
  minPrice$ = new BehaviorSubject<number>(0);
  maxPrice$ = new BehaviorSubject<number>(10000);
  minRating$ = new BehaviorSubject<number>(0);

  // UI States
  isFilterOpen = false;
  isSortOpen = false;
  title: string = '';
  isCategoryPage = false;
  currentCategory = 'all';

  constructor(
    private productService: ProductService,
    public cartService: CartService,
    public wishlistService: WishlistService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.categories$ = this.productService.getCategories();
    this.products$ = this.route.params.pipe(
      switchMap(params => {
        const type = this.route.snapshot.data['type'];
        const name = params['name'];
        console.log(`Products Page: type=${type}, name=${name}`);

        let request: Observable<ProductList[]>;
        if (type === 'store') {
          this.title = `${name} Mağazası Ürünleri`;
          request = this.productService.getProductsByStore(name);
        } else if (type === 'brand') {
          this.title = `${name} Marka Ürünler`;
          request = this.productService.getProductsByBrand(name);
        } else if (type === 'category') {
          this.title = `${name} Kategorisi`;
          this.isCategoryPage = true;
          this.currentCategory = name;
          this.selectedCategory$.next(name);
          request = this.productService.getProductsByCategory(name);
        } else {
          this.title = 'Tüm Ürünler';
          this.isCategoryPage = false;
          this.selectedCategory$.next('all');
          request = this.productService.getProducts();
        }

        return request.pipe(
          tap(products => console.log('Products from API:', products)),
          catchError(err => {
            console.error('API Error in Products:', err);
            return of([]);
          })
        );
      })
    );

    this.filteredProducts$ = combineLatest([
      this.products$,
      this.searchQuery$,
      this.selectedCategory$,
      this.minPrice$,
      this.maxPrice$,
      this.minRating$,
      this.sortOption$
    ]).pipe(
      map(([products, search, category, minP, maxP, minR, sort]) => {
        let filtered = products.filter(p => {
          const nameMatch = (p.name || '').toLowerCase().includes(search.toLowerCase());
          const categoryMatch = category === 'all' || 
                                !p.categoryName || 
                                p.categoryName.toLowerCase() === category.toLowerCase();
          const priceMatch = (p.price ?? 0) >= minP && (p.price ?? 0) <= maxP;
          const ratingMatch = (p.rating ?? 0) >= minR;
          
          return nameMatch && categoryMatch && priceMatch && ratingMatch;
        });

        console.log(`Filtered count: ${filtered.length} (out of ${products.length})`);

        // Sorting
        switch(sort) {
          case 'name-asc': filtered.sort((a,b) => a.name.localeCompare(b.name)); break;
          case 'name-desc': filtered.sort((a,b) => b.name.localeCompare(a.name)); break;
          case 'price-asc': filtered.sort((a,b) => a.price - b.price); break;
          case 'price-desc': filtered.sort((a,b) => b.price - a.price); break;
          case 'rating-desc': filtered.sort((a,b) => b.rating - a.rating); break;
          case 'rating-asc': filtered.sort((a,b) => a.rating - b.rating); break;
        }

        return filtered;
      })
    );
  }

  // Event Handlers
  updateSearch(event: Event) { this.searchQuery$.next((event.target as HTMLInputElement).value); }
  updateCategory(cat: string) { if (!this.isCategoryPage || cat === this.currentCategory) this.selectedCategory$.next(cat); }
  
  updateMinPrice(event: Event) {
    const val = +(event.target as HTMLInputElement).value;
    if (val <= this.maxPrice$.value) this.minPrice$.next(val);
    else { (event.target as HTMLInputElement).value = this.maxPrice$.value.toString(); this.minPrice$.next(this.maxPrice$.value); }
  }

  updateMaxPrice(event: Event) {
    const val = +(event.target as HTMLInputElement).value;
    if (val >= this.minPrice$.value) this.maxPrice$.next(val);
    else { (event.target as HTMLInputElement).value = this.minPrice$.value.toString(); this.maxPrice$.next(this.minPrice$.value); }
  }

  updateRating(event: Event) { this.minRating$.next(+(event.target as HTMLInputElement).value); }
  
  toggleFilters() { this.isFilterOpen = !this.isFilterOpen; if (this.isFilterOpen) this.isSortOpen = false; }
  toggleSort() { this.isSortOpen = !this.isSortOpen; if (this.isSortOpen) this.isFilterOpen = false; }
  selectSort(opt: string) { this.sortOption$.next(opt); this.isSortOpen = false; }
}


