package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.BrandDTO;
import com.example.onlyfanshop_be.dto.CategoryDTO;
import com.example.onlyfanshop_be.dto.Pagination;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.response.HomepageResponse;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.BrandRepository;
import com.example.onlyfanshop_be.repository.CategoryRepository;
import com.example.onlyfanshop_be.entity.Brand;
import com.example.onlyfanshop_be.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.example.onlyfanshop_be.dto.ProductDTO;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class ProductService implements  IProductService {
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private BrandRepository brandRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public ApiResponse<Object> getHomepage(String keyword, Integer categoryId, Integer brandId, int page, int size, String sortBy, String order) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));

        Specification<Product> spec = Specification.anyOf();

        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
        }

        if (categoryId != null && categoryId > 0) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("categoryID"), categoryId));
        }

        if (brandId != null && brandId > 0) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("brand").get("brandID"), brandId));
        }

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<ProductDTO> productDTOs = productPage.getContent().stream()
                .map(p -> ProductDTO.builder()
                        .productId(p.getProductId())
                        .productName(p.getProductName())
                        .briefDescription(p.getBriefDescription())
                        .fullDescription(p.getFullDescription())
                        .technicalSpecifications(p.getTechnicalSpecifications())
                        .price(p.getPrice())
                        .imageUrl(p.getImageUrl())
                        .brand(p.getBrand() != null ? p.getBrand().getBrandName() : null)
                        .brandDisplayName(p.getBrand() != null ? p.getBrand().getBrandName() : null)
                        .category(p.getCategory() != null ? p.getCategory().getCategoryName() : null)
                        .categoryDisplayName(p.getCategory() != null ? p.getCategory().getCategoryName() : null)
                        .build()
                )
                .toList();

        // Get selected category and brand names
        String selectedCategoryName = "All";
        String selectedBrandName = "All";
        
        if (categoryId != null && categoryId > 0) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                selectedCategoryName = category.getCategoryName();
            }
        }
        
        if (brandId != null && brandId > 0) {
            Brand brand = brandRepository.findById(brandId).orElse(null);
            if (brand != null) {
                selectedBrandName = brand.getBrandName();
            }
        }

        HomepageResponse.Filters filters = HomepageResponse.Filters.builder()
                .selectedCategory(selectedCategoryName)
                .selectedBrand(selectedBrandName)
                .sortOption(sortBy + "_" + order.toLowerCase())
                .build();

        // Convert entities to DTOs
        List<CategoryDTO> categories = categoryRepository.findAll().stream()
                .map(c -> new CategoryDTO(c.getCategoryID(), c.getCategoryName()))
                .toList();

        List<BrandDTO> brands = brandRepository.findAll().stream()
                .map(b -> new BrandDTO((long)b.getBrandID(), b.getBrandName()))
                .toList();

        Pagination pagination = Pagination.builder()
                .page(page)
                .size(size)
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .build();

        return ApiResponse.builder().data(HomepageResponse.builder()
                .filters(filters)
                .categories(categories)
                .brands(brands)
                .products(productDTOs)
                .pagination(pagination)
                .build()).build();
                
    }


}
