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
import com.shop.ecommerce.repository.ReviewRepository;
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
    private final ReviewRepository reviewRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          BrandRepository brandRepository, StoreRepository storeRepository,
                          ReviewRepository reviewRepository){
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.storeRepository = storeRepository;
        this.reviewRepository = reviewRepository;
    }

    public List<String> getCategories(){
        return categoryRepository.findAll().stream().map(c -> c.getName()).toList();
    }

    public List<ProductListDTO> getProducts(){
        return productRepository.findAllWithRatings();
    }

    public ProductDetailDTO getProductByID(Long id){
        Product product = productRepository.findById(id).orElseThrow();
        return toDetailDTOWithRating(product);
    }

    public List<ProductListDTO> getProductsByStore(String store){
        return productRepository.findByStoreWithRatings(store);
    }

    public List<ProductListDTO> getProductsByBrand(String brand){
        return productRepository.findByBrandWithRatings(brand);
    }

    public List<ProductListDTO> getProductsByCategory(String category){
        return productRepository.findByCategoryWithRatings(category);
    }

    public ProductDetailDTO createProduct(ProductCreateDTO dto){
        Product product = ProductMapper.toEntity(dto);
        Product savedProduct = productRepository.save(product);
        return toDetailDTOWithRating(savedProduct);
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
        return toDetailDTOWithRating(updatedProduct);
    }

    public List<ProductListDTO> getTopRatedProducts(int limit){
        return productRepository.findTopRatedWithRatings(PageRequest.of(0, limit));
    }

    public List<ProductListDTO> getBestSellingProducts(int limit){
        return productRepository.findAllByOrderBySoldCountDesc(PageRequest.of(0, limit))
                .stream().map(this::toListDTOWithRating).toList();
    }

    public Map<String, Long> getHomeStats(){
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("categoryCount", categoryRepository.count());
        stats.put("brandCount", brandRepository.count());
        stats.put("storeCount", storeRepository.count());
        stats.put("productCount", productRepository.count());
        return stats;
    }

    private ProductListDTO toListDTOWithRating(Product product) {
        double avgRating = reviewRepository.averageRatingByProductId(product.getId());
        long reviewCount = reviewRepository.countByProductId(product.getId());
        return ProductMapper.toListDTO(product, avgRating, reviewCount);
    }

    private ProductDetailDTO toDetailDTOWithRating(Product product) {
        double avgRating = reviewRepository.averageRatingByProductId(product.getId());
        long reviewCount = reviewRepository.countByProductId(product.getId());
        return ProductMapper.toDetailDTO(product, avgRating, reviewCount);
    }
}
