package com.shop.ecommerce.controller;

import com.shop.ecommerce.entities.*;
import com.shop.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @GetMapping
    public ResponseEntity<Page<Product>> getProducts(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(productRepository.findByStoreId(storeId, pageable));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @GetMapping("/brands")
    public ResponseEntity<List<Brand>> getBrands() {
        return ResponseEntity.ok(brandRepository.findAll());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Product> getProductById(@PathVariable Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<Product> addProduct(@RequestBody Map<String, Object> body, @RequestParam Long storeId) {
        Product product = new Product();
        product.setName((String) body.get("name"));
        product.setDescription((String) body.get("description"));
        product.setPrice(new java.math.BigDecimal(body.get("price").toString()));
        product.setImg((String) body.get("img"));

        Store store = new Store();
        store.setId(storeId);
        product.setStore(store);

        if (body.containsKey("categoryId")) {
            categoryRepository.findById(Long.parseLong(body.get("categoryId").toString()))
                    .ifPresent(product::setCategory);
        }
        if (body.containsKey("brandId")) {
            Brand brand = new Brand();
            brand.setId(Long.parseLong(body.get("brandId").toString()));
            product.setBrand(brand);
        }

        Product saved = productRepository.save(product);

        Inventory inventory = new Inventory();
        inventory.setProduct(saved);
        inventory.setStoreId(storeId);
        inventory.setStock(body.containsKey("stock") ? Integer.parseInt(body.get("stock").toString()) : 0);
        inventoryRepository.save(inventory);

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long productId, @RequestBody Map<String, Object> body) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (body.containsKey("name")) product.setName((String) body.get("name"));
        if (body.containsKey("description")) product.setDescription((String) body.get("description"));
        if (body.containsKey("price")) product.setPrice(new java.math.BigDecimal(body.get("price").toString()));
        if (body.containsKey("img")) product.setImg((String) body.get("img"));

        if (body.containsKey("stock") && product.getInventory() != null) {
            product.getInventory().setStock(Integer.parseInt(body.get("stock").toString()));
            inventoryRepository.save(product.getInventory());
        }

        return ResponseEntity.ok(productRepository.save(product));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long productId) {
        productRepository.deleteById(productId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}

