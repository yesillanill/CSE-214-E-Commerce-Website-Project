package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.product.ProductCreateDTO;
import com.shop.ecommerce.dto.product.ProductDetailDTO;
import com.shop.ecommerce.dto.product.ProductListDTO;
import com.shop.ecommerce.dto.product.ProductUpdateDTO;
import com.shop.ecommerce.entities.Product;
import com.shop.ecommerce.entities.Brand;
import com.shop.ecommerce.entities.Category;
import com.shop.ecommerce.entities.Store;
import com.shop.ecommerce.mapper.ProductMapper;
import com.shop.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository){
        this.productRepository = productRepository;
    }

    public List<ProductListDTO> getProducts(){
        return productRepository.findAll().stream().map(ProductMapper::toListDTO).toList();
    }

    public ProductDetailDTO getProductByID(Long id){
        Product product = productRepository.findById(id).orElseThrow();
        return ProductMapper.toDetailDTO(product);
    }

    public List<ProductListDTO> getProductsByStore(String store){
        return productRepository.findByStoreName(store).stream().map(ProductMapper::toListDTO).toList();
    }

    public List<ProductListDTO> getProductsByBrand(String brand){
        return productRepository.findByBrandName(brand).stream().map(ProductMapper::toListDTO).toList();
    }

    public List<ProductListDTO> getProductsByCategory(String category){
        return productRepository.findByCategoryName(category).stream().map(ProductMapper::toListDTO).toList();
    }

    public ProductDetailDTO createProduct(ProductCreateDTO dto){
        Product product = ProductMapper.toEntity(dto);

        Product savedProduct = productRepository.save(product);

        return ProductMapper.toDetailDTO(savedProduct);
    }

    public void deleteProduct(Long id){
        productRepository.deleteById(id);
    }

    public ProductDetailDTO updateProduct(Long id, ProductUpdateDTO dto){
        Product product =  productRepository.findById(id).orElseThrow();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setImg(dto.getImg());
        Product updatedProduct = productRepository.save(product);
        return ProductMapper.toDetailDTO(updatedProduct);
    }
}
