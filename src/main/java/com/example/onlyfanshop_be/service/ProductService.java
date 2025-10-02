package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.BrandDTO;
import com.example.onlyfanshop_be.dto.CategoryDTO;
import com.example.onlyfanshop_be.dto.Pagination;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.response.HomepageResponse;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.repository.BrandRepository;
import com.example.onlyfanshop_be.repository.CategoryRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.example.onlyfanshop_be.dto.ProductDTO;
import com.example.onlyfanshop_be.dto.ProductDetailDTO;
import com.example.onlyfanshop_be.entity.Category;
import com.example.onlyfanshop_be.entity.Brand;
import org.springframework.stereotype.Service;


import java.util.List;

import static org.springframework.http.codec.ServerSentEvent.builder;

@Service
@RequiredArgsConstructor
public class ProductService implements  IProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private BrandRepository brandRepository;

    @Override
    public ApiResponse<HomepageResponse> getHomepage(String keyword, Integer categoryId, Integer brandId, int page, int size, String sortBy, String order) {
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
                        .id(p.getProductID())
                        .productName(p.getProductName())
                        .price(p.getPrice())
                        .imageURL(p.getImageURL())
                        .briefDescription(p.getBriefDescription())
                        .brand(BrandDTO.builder()
                                .brandID(p.getBrand().getBrandID() == null ? null : p.getBrand().getBrandID().longValue())
                                .name(p.getBrand().getBrandName())
                                .build())
                        .category(new CategoryDTO(
                                p.getCategory().getCategoryID(),
                                p.getCategory().getCategoryName()
                        ))
                        .build()
                )
                .toList();

        HomepageResponse.Filters filters = HomepageResponse.Filters.builder()
                .selectedCategory(categoryId != null && categoryId > 0 ? categoryRepository.findById(categoryId).map(Category::getCategoryName).orElse("All") : "All")
                .selectedBrand(brandId != null && brandId > 0 ? brandRepository.findById(brandId).map(Brand::getBrandName).orElse("All") : "All")
                .sortOption(sortBy + "_" + order.toLowerCase())
                .build();

        List<CategoryDTO> categories = categoryRepository.findAll().stream()
                .map(c -> new CategoryDTO(c.getCategoryID(), c.getCategoryName()))
                .toList();

        List<BrandDTO> brands = brandRepository.findAll().stream()
                .map(b -> BrandDTO.builder()
                        .brandID(b.getBrandID() == null ? null : b.getBrandID().longValue())
                        .name(b.getBrandName())
                        .build())
                .toList();

        Pagination pagination = Pagination.builder()
                .page(page)
                .size(size)
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .build();

        return ApiResponse.<HomepageResponse>builder().statusCode(200).data(HomepageResponse.builder()
                .filters(filters)
                .categories(categories)
                .brands(brands)
                .products(productDTOs)
                .pagination(pagination)
                .build()).build();

    }

    @Override
    public ApiResponse<ProductDetailDTO> getProductDetail(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductDetailDTO dto = ProductDetailDTO.builder()
                .id(product.getProductID())
                .productName(product.getProductName())
                .briefDescription(product.getBriefDescription())
                .fullDescription(product.getFullDescription())
                .technicalSpecifications(product.getTechnicalSpecifications())
                .price(product.getPrice())
                .imageURL(product.getImageURL())
                .brand(BrandDTO.builder()
                        .brandID(product.getBrand().getBrandID() == null ? null : product.getBrand().getBrandID().longValue())
                        .name(product.getBrand().getBrandName())
                        .build())
                .category(new CategoryDTO(
                        product.getCategory().getCategoryID(),
                        product.getCategory().getCategoryName()
                ))
                .build();

        return ApiResponse.<ProductDetailDTO>builder()
                .data(dto)
                .build();
    }


}
