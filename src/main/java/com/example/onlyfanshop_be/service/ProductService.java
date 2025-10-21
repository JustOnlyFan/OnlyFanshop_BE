package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.BrandDTO;
import com.example.onlyfanshop_be.dto.CategoryDTO;
import com.example.onlyfanshop_be.dto.Pagination;
import com.example.onlyfanshop_be.dto.request.ProductDetailRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.response.HomepageResponse;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
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
import java.util.Optional;

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

        Specification<Product> spec = (root, query, cb) -> cb.isTrue(root.get("isActive"));
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
                                .brandID(p.getBrand().getBrandID() == null ? null : p.getBrand().getBrandID().intValue())
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
                        .brandID(b.getBrandID() == null ? null : b.getBrandID().intValue())
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
                        .brandID(product.getBrand().getBrandID() == null ? null : product.getBrand().getBrandID().intValue())
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
    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    @Override
    public ProductDetailDTO getProductById(int id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + id));

        // Ánh xạ từ Entity -> DTO
        return ProductDetailDTO.builder()
                .id(product.getProductID())
                .productName(product.getProductName())
                .briefDescription(product.getBriefDescription())
                .fullDescription(product.getFullDescription())
                .technicalSpecifications(product.getTechnicalSpecifications())
                .price(product.getPrice())
                .imageURL(product.getImageURL())
                .brand(product.getBrand() != null
                        ? new BrandDTO(
                        product.getBrand().getBrandID(),
                        product.getBrand().getBrandName()
                )
                        : null)
                .category(product.getCategory() != null
                        ? new CategoryDTO(
                        product.getCategory().getCategoryID(),
                        product.getCategory().getCategoryName()
                )
                        : null)
                .build();
    }

    @Override
    public Product createProduct(ProductDetailRequest request) {
        // 1️⃣ Kiểm tra danh mục
        Category category = null;
        if (request.getCategoryID() != null) {
            category = categoryRepository.findById(request.getCategoryID())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục có ID: " + request.getCategoryID()));
        }

        // 2️⃣ Kiểm tra thương hiệu
        Brand brand = null;
        if (request.getBrandID() != null) {
            brand = brandRepository.findById(request.getBrandID())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu có ID: " + request.getBrandID()));
        }

        // 3️⃣ Tạo Product entity
        Product product = Product.builder()
                .productName(request.getProductName())
                .briefDescription(request.getBriefDescription())
                .fullDescription(request.getFullDescription())
                .technicalSpecifications(request.getTechnicalSpecifications())
                .price(request.getPrice())
                .imageURL(request.getImageURL())
                .category(category)
                .brand(brand)
                .build();

        // 4️⃣ Lưu vào DB
        return productRepository.save(product);
    }

    @Override
    public ProductDetailDTO updateProduct(Integer id, ProductDetailRequest updatedProduct) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + id));

        product.setProductName(updatedProduct.getProductName());
        product.setBriefDescription(updatedProduct.getBriefDescription());
        product.setFullDescription(updatedProduct.getFullDescription());
        product.setTechnicalSpecifications(updatedProduct.getTechnicalSpecifications());
        product.setPrice(updatedProduct.getPrice());

        // Cập nhật Category (nếu có)
        if (updatedProduct.getCategoryID()!= null) {
            Category category = categoryRepository.findById(updatedProduct.getCategoryID())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục có ID: " + updatedProduct.getCategoryID()));
            product.setCategory(category);
        }

        // Cập nhật Brand (nếu có)
        if (updatedProduct.getBrandID()!= null) {
            Brand brand = brandRepository.findById(updatedProduct.getBrandID())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu có ID: " + updatedProduct.getBrandID()));
            product.setBrand(brand);
        }

        Product savedProduct = productRepository.save(product);

        // Trả về DTO
        return ProductDetailDTO.builder()
                .id(savedProduct.getProductID())
                .productName(savedProduct.getProductName())
                .briefDescription(savedProduct.getBriefDescription())
                .fullDescription(savedProduct.getFullDescription())
                .technicalSpecifications(savedProduct.getTechnicalSpecifications())
                .price(savedProduct.getPrice())
                .imageURL(savedProduct.getImageURL())
                .brand(savedProduct.getBrand() != null ? new BrandDTO(
                        savedProduct.getBrand().getBrandID(),
                        savedProduct.getBrand().getBrandName()

                ) : null)
                .category(savedProduct.getCategory() != null ? new CategoryDTO(
                        savedProduct.getCategory().getCategoryID(),
                        savedProduct.getCategory().getCategoryName()
                ) : null)
                .build();
    }

    @Override
    public void deleteProduct(int id) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            product.setActive(false);
            productRepository.save(product);
        }else throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
    }
    @Override
    public void updateImage(int productId, String imageURL) {
        Optional<Product> product = productRepository.findById(productId);
        if(product.isPresent()) {
            product.get().setImageURL(imageURL);
            productRepository.save(product.get());
        }else throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
    }

    @Override
    public ApiResponse<HomepageResponse> productList(String keyword, Integer categoryId, Integer brandId, int page, int size, String sortBy, String order) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));

        Specification<Product> spec =Specification.anyOf();
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
                                .brandID(p.getBrand().getBrandID() == null ? null : p.getBrand().getBrandID().intValue())
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
                        .brandID(b.getBrandID() == null ? null : b.getBrandID().intValue())
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
    public void updateActive(int productId, boolean active) {
        Optional<Product> product = productRepository.findById(productId);
        if(product.isPresent()) {
            product.get().setActive(active);
            productRepository.save(product.get());
        }else throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
    }
}
