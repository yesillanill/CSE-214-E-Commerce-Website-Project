package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.product.ProductCreateDTO;
import com.shop.ecommerce.dto.product.ProductDetailDTO;
import com.shop.ecommerce.dto.product.ProductListDTO;
import com.shop.ecommerce.dto.product.ProductUpdateDTO;
import com.shop.ecommerce.entities.Product;
import com.shop.ecommerce.mapper.ProductMapper;
import com.shop.ecommerce.repository.BrandRepository;
import com.shop.ecommerce.repository.CategoryRepository;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.StoreRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final StoreRepository storeRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          BrandRepository brandRepository, StoreRepository storeRepository){
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.storeRepository = storeRepository;
    }

    public List<String> getCategories(){
        return categoryRepository.findAll().stream().map(c -> c.getName()).toList();
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
        product.setPrice(dto.getPrice() != null ? java.math.BigDecimal.valueOf(dto.getPrice()) : null);
        product.setImg(dto.getImg());
        Product updatedProduct = productRepository.save(product);
        return ProductMapper.toDetailDTO(updatedProduct);
    }

    public List<ProductListDTO> getTopRatedProducts(int limit){
        return productRepository.findAllByOrderByRatingDesc(PageRequest.of(0, limit))
                .stream().map(ProductMapper::toListDTO).toList();
    }

    public List<ProductListDTO> getBestSellingProducts(int limit){
        return productRepository.findAllByOrderBySoldCountDesc(PageRequest.of(0, limit))
                .stream().map(ProductMapper::toListDTO).toList();
    }

    public Map<String, Long> getHomeStats(){
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("categoryCount", categoryRepository.count());
        stats.put("brandCount", brandRepository.count());
        stats.put("storeCount", storeRepository.count());
        stats.put("productCount", productRepository.count());
        return stats;
    }
}
