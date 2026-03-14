import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { ProductList } from '../../../core/models/product-list.model';
import { Observable, switchMap } from 'rxjs';
//import { ProductCard } from "../product-card/product-card";

@Component({
  selector: 'app-products',
  templateUrl: './products.html',
  styleUrl: './products.scss',
  imports: []
})
export class Products implements OnInit {
  products$!: Observable<ProductList[]>;
  title: string = '';

  constructor(
    private productService: ProductService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
  this.products$ = this.route.params.pipe(
    switchMap(params => {
      const type = this.route.snapshot.data['type'];
      const name = params['name'];

      if (type === 'store') {
        this.title = `${name} Mağazası Ürünleri`;
        return this.productService.getProductsByStore(name);
      }

      if (type === 'brand') {
        this.title = `${name} Marka Ürünler`;
        return this.productService.getProductsByBrand(name);
      }

      if (type === 'category') {
        this.title = `${name} Kategorisi`;
        return this.productService.getProductsByCategory(name);
      }

      // Default: Tüm Ürünler
      this.title = 'Tüm Ürünler';
      return this.productService.getProducts();
    })
  );
}
}
