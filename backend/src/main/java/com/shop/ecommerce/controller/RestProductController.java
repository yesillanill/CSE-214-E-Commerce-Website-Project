package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.product.ProductCreateDTO;
import com.shop.ecommerce.dto.product.ProductDetailDTO;
import com.shop.ecommerce.dto.product.ProductListDTO;
import com.shop.ecommerce.dto.product.ProductUpdateDTO;
import com.shop.ecommerce.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "*")
public class RestProductController {

    @Autowired
    private ProductService productService;

    public RestProductController(ProductService productService){
        this.productService = productService;
    }

    @GetMapping
    public List<ProductListDTO> getProducts(){
        return productService.getProducts();
    }

    @GetMapping(path="/{id}")
    public ProductDetailDTO getProductByID(@PathVariable Long id){
        return productService.getProductByID(id);
    }

    @GetMapping(path="/store/{storeName}")
    public List<ProductListDTO> getProductsByStore(@PathVariable String storeName){
        return productService.getProductsByStore(storeName);
    }

    @GetMapping(path="/category/{categoryName}")
    public List<ProductListDTO> getProductsByCategory(@PathVariable String categoryName){
        return productService.getProductsByCategory(categoryName);
    }

    @GetMapping(path="/brand/{brandName}")
    public List<ProductListDTO> getProductsByBrand(@PathVariable String brandName){
        return productService.getProductsByBrand(brandName);
    }

    @PostMapping
    public ProductDetailDTO createProduct(@RequestBody ProductCreateDTO dto){
        return productService.createProduct(dto);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id){
        productService.deleteProduct(id);
    }

    @PutMapping("/{id}")
    public ProductDetailDTO updateProduct(@PathVariable Long id, @RequestBody ProductUpdateDTO dto){
        return productService.updateProduct(id,dto);
    }
}
