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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService implements  IProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private com.example.onlyfanshop_be.repository.ColorRepository colorRepository;
    @Autowired
    private com.example.onlyfanshop_be.repository.WarrantyRepository warrantyRepository;
    @Autowired
    private com.example.onlyfanshop_be.repository.ProductImageRepository productImageRepository;
    @Autowired
    private StoreInventoryService storeInventoryService;

    @Override
    public ApiResponse<HomepageResponse> getHomepage(
            String keyword, Integer categoryId, Integer brandId,
            Long minPrice, Long maxPrice, Integer bladeCount,
            Boolean remoteControl, Boolean oscillation, Boolean timer,
            Integer minPower, Integer maxPower,
            int page, int size, String sortBy, String order) {
        try {
            System.out.println("ProductService.getHomepage - sortBy: " + sortBy);
            Sort.Direction direction = "DESC".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
            String actualSortField = mapSortField(sortBy);
            System.out.println("ProductService.getHomepage - mapped sortBy: " + actualSortField);
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, actualSortField));

            Specification<Product> spec = (root, query, cb) -> 
                    cb.equal(root.get("status"), com.example.onlyfanshop_be.enums.ProductStatus.active);
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and((root, query, cb) ->
                        cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"));
            }

            if (categoryId != null && categoryId > 0) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("categoryId"), categoryId));
            }

            if (brandId != null && brandId > 0) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("brandId"), brandId));
            }

            // Filter theo giá
            if (minPrice != null) {
                spec = spec.and((root, query, cb) ->
                        cb.greaterThanOrEqualTo(root.get("basePrice"), java.math.BigDecimal.valueOf(minPrice)));
            }
            if (maxPrice != null) {
                spec = spec.and((root, query, cb) ->
                        cb.lessThanOrEqualTo(root.get("basePrice"), java.math.BigDecimal.valueOf(maxPrice)));
            }

            // Filter theo số cánh quạt
            if (bladeCount != null && bladeCount > 0) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("bladeCount"), bladeCount));
            }

            // Filter theo tiện ích
            if (remoteControl != null && remoteControl) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("remoteControl"), true));
            }
            if (oscillation != null && oscillation) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("oscillation"), true));
            }
            if (timer != null && timer) {
                spec = spec.and((root, query, cb) ->
                        cb.isNotNull(root.get("timer")));
            }

            // Filter theo công suất
            if (minPower != null) {
                spec = spec.and((root, query, cb) ->
                        cb.greaterThanOrEqualTo(root.get("powerWatt"), minPower));
            }
            if (maxPower != null) {
                spec = spec.and((root, query, cb) ->
                        cb.lessThanOrEqualTo(root.get("powerWatt"), maxPower));
            }

            Page<Product> productPage = productRepository.findAll(spec, pageable);
            List<Product> products = productPage.getContent();
            
            // Load all product images in batch to avoid N+1 query problem
            java.util.Map<Long, String> productImageMap = loadProductImagesBatch(products);

            List<ProductDTO> productDTOs = products.stream()
                    .map(p -> {
                        BrandDTO brandDTO = null;
                        if (p.getBrand() != null) {
                            brandDTO = BrandDTO.builder()
                                    .brandID(p.getBrand().getBrandID() == null ? null : p.getBrand().getBrandID().intValue())
                                    .name(p.getBrand().getBrandName())
                                    .imageURL(p.getBrand().getImageURL())
                                    .build();
                        }
                        
                        CategoryDTO categoryDTO = null;
                        if (p.getCategory() != null) {
                            categoryDTO = new CategoryDTO(
                                    p.getCategory().getCategoryID(),
                                    p.getCategory().getCategoryName()
                            );
                        }
                        
                        // Get image URL from map (already loaded in batch)
                        String imageURL = productImageMap.get(p.getId());
                        
                        return ProductDTO.builder()
                                .id(p.getProductID())
                                .productName(p.getProductName())
                                .price(p.getPrice())
                                .imageURL(imageURL)
                                .briefDescription(p.getBriefDescription())
                                .brand(brandDTO)
                                .category(categoryDTO)
                                .build();
                    })
                    .toList();
            java.math.BigDecimal maxPriceBD = productRepository.findMaxPrice();
            java.math.BigDecimal minPriceBD = productRepository.findMinPrice();
            Long maxPriceFilter = maxPriceBD != null ? maxPriceBD.longValue() : null;
            Long minPriceFilter = minPriceBD != null ? minPriceBD.longValue() : null;
            HomepageResponse.Filters filters = HomepageResponse.Filters.builder()
                    .selectedCategory(categoryId != null && categoryId > 0 ? categoryRepository.findById(categoryId).map(Category::getCategoryName).orElse("All") : "All")
                    .selectedBrand(brandId != null && brandId > 0 ? brandRepository.findById(brandId).map(Brand::getBrandName).orElse("All") : "All")
                    .sortOption(sortBy + "_" + order.toLowerCase())
                    .maxPrice(maxPriceFilter)
                    .minPrice(minPriceFilter)
                    .build();

            List<CategoryDTO> categories = categoryRepository.findAll().stream()
                    .map(c -> new CategoryDTO(c.getCategoryID(), c.getCategoryName()))
                    .toList();

            List<BrandDTO> brands = brandRepository.findAll().stream()
                    .map(b -> BrandDTO.builder()
                            .brandID(b.getBrandID() == null ? null : b.getBrandID().intValue())
                            .name(b.getBrandName())
                            .imageURL(b.getImageURL())
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
        } catch (Exception e) {
            System.err.println("Error in ProductService.getHomepage: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi lấy trang chủ: " + e.getMessage(), e);
        }
    }

    @Override
    public ApiResponse<ProductDetailDTO> getProductDetail(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Sử dụng helper method để build DTO với tất cả các field mới
        ProductDetailDTO dto = buildProductDetailDTO(product);

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

        // Ánh xạ từ Entity -> DTO với tất cả các field mới
        return buildProductDetailDTO(product);
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

        // 3️⃣ Generate slug từ productName (luôn tự động generate, không nhận từ request)
        String slug = generateSlug(request.getProductName());

        // 4️⃣ Generate SKU (luôn tự động generate, không nhận từ request)
        if (brand == null) {
            throw new RuntimeException("Không thể tạo SKU: Sản phẩm phải có thương hiệu để tự động sinh mã SKU");
        }
        String sku = generateSku(brand.getId(), brand.getName());
        
        // 5️⃣ Kiểm tra và load colors
        List<com.example.onlyfanshop_be.entity.Color> colors = new java.util.ArrayList<>();
        if (request.getColorIds() != null && !request.getColorIds().isEmpty()) {
            colors = colorRepository.findAllById(request.getColorIds());
            if (colors.size() != request.getColorIds().size()) {
                throw new RuntimeException("Một hoặc nhiều màu sắc không tồn tại");
            }
        }
        
        // 6️⃣ Kiểm tra và load warranty
        com.example.onlyfanshop_be.entity.Warranty warranty = null;
        if (request.getWarrantyId() != null) {
            warranty = warrantyRepository.findById(request.getWarrantyId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bảo hành có ID: " + request.getWarrantyId()));
        }

        // 7️⃣ Tạo Product entity với tất cả các field mới
        Product product = Product.builder()
                .name(request.getProductName())
                .slug(slug)
                .sku(sku)
                .shortDescription(request.getBriefDescription())
                .description(request.getFullDescription())
                .basePrice(request.getPrice() != null ? java.math.BigDecimal.valueOf(request.getPrice()) : null)
                .categoryId(category != null ? category.getId() : null)
                .brandId(brand != null ? brand.getId() : null)
                .powerWatt(request.getPowerWatt())
                .bladeDiameterCm(request.getBladeDiameterCm())
                // Technical specifications
                .voltage(request.getVoltage())
                .windSpeedLevels(request.getWindSpeedLevels())
                .airflow(request.getAirflow())
                .bladeMaterial(request.getBladeMaterial())
                .bodyMaterial(request.getBodyMaterial())
                .bladeCount(request.getBladeCount())
                .noiseLevel(request.getNoiseLevel())
                .motorSpeed(request.getMotorSpeed())
                .weight(request.getWeight())
                .adjustableHeight(request.getAdjustableHeight())
                // Features
                .remoteControl(request.getRemoteControl() != null ? request.getRemoteControl() : false)
                .timer(request.getTimer())
                .naturalWindMode(request.getNaturalWindMode() != null ? request.getNaturalWindMode() : false)
                .sleepMode(request.getSleepMode() != null ? request.getSleepMode() : false)
                .oscillation(request.getOscillation() != null ? request.getOscillation() : false)
                .heightAdjustable(request.getHeightAdjustable() != null ? request.getHeightAdjustable() : false)
                .autoShutoff(request.getAutoShutoff() != null ? request.getAutoShutoff() : false)
                .temperatureSensor(request.getTemperatureSensor() != null ? request.getTemperatureSensor() : false)
                .energySaving(request.getEnergySaving() != null ? request.getEnergySaving() : false)
                // Other information
                .safetyStandards(request.getSafetyStandards())
                .manufacturingYear(request.getManufacturingYear())
                .accessories(request.getAccessories())
                .energyRating(request.getEnergyRating())
                // Legacy fields
                .colorDefault(request.getColorDefault()) // Legacy field
                .warrantyId(warranty != null ? warranty.getId() : null)
                .warrantyMonths(request.getWarrantyMonths()) // Legacy field
                .quantity(request.getQuantity() != null ? request.getQuantity() : 0) // Số lượng sản phẩm
                .status(com.example.onlyfanshop_be.enums.ProductStatus.active)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        // 8️⃣ Lưu product vào DB trước
        Product savedProduct = productRepository.save(product);
        
        // 9️⃣ Set colors relationship (sau khi product đã được save)
        if (!colors.isEmpty()) {
            savedProduct.setColors(colors);
            savedProduct = productRepository.save(savedProduct);
        }
        
        // 🔟 Tạo ProductImage nếu có imageURL (save trực tiếp để đảm bảo lưu vào DB)
        if (request.getImageURL() != null && !request.getImageURL().trim().isEmpty()) {
            try {
                // Tạo ProductImage entity
                com.example.onlyfanshop_be.entity.ProductImage productImage = com.example.onlyfanshop_be.entity.ProductImage.builder()
                        .productId(savedProduct.getId())
                        .imageUrl(request.getImageURL().trim())
                        .isMain(true) // Đặt làm ảnh chính
                        .sortOrder(0)
                        .build();
                
                // Save trực tiếp vào database (không dựa vào cascade)
                productImageRepository.save(productImage);
                
                System.out.println("ProductService: Created ProductImage with URL: " + request.getImageURL());
                System.out.println("ProductService: ProductImage saved with ID: " + productImage.getId() + ", ProductId: " + productImage.getProductId());
            } catch (Exception e) {
                System.err.println("ProductService: Error saving ProductImage: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Không thể lưu ảnh sản phẩm: " + e.getMessage(), e);
            }
        }

        // 🔟➕ Tự động add product vào tất cả stores (isAvailable = true)
        try {
            storeInventoryService.addProductToAllStores(savedProduct.getId());
            System.out.println("ProductService: Added product " + savedProduct.getId() + " to all stores");
        } catch (Exception e) {
            // Log error but don't fail product creation
            System.err.println("ProductService: Error adding product to stores: " + e.getMessage());
            e.printStackTrace();
        }

        return savedProduct;
    }

    @Override
    public ProductDetailDTO updateProduct(Integer id, ProductDetailRequest updatedProduct) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + id));

        // Cập nhật các field cơ bản
        if (updatedProduct.getProductName() != null) {
            product.setName(updatedProduct.getProductName());
            // Regenerate slug khi productName thay đổi (slug luôn tự động generate)
            product.setSlug(generateSlug(updatedProduct.getProductName()));
        }
        
        // Slug và SKU không thể update trực tiếp (luôn tự động generate)
        
        if (updatedProduct.getBriefDescription() != null) {
            product.setShortDescription(updatedProduct.getBriefDescription());
        }
        
        if (updatedProduct.getFullDescription() != null) {
            product.setDescription(updatedProduct.getFullDescription());
        }
        
        if (updatedProduct.getPrice() != null) {
            product.setBasePrice(java.math.BigDecimal.valueOf(updatedProduct.getPrice()));
        }
        
        // Cập nhật các field mới
        if (updatedProduct.getPowerWatt() != null) {
            product.setPowerWatt(updatedProduct.getPowerWatt());
        }
        
        if (updatedProduct.getBladeDiameterCm() != null) {
            product.setBladeDiameterCm(updatedProduct.getBladeDiameterCm());
        }
        
        // Cập nhật technical specifications
        if (updatedProduct.getVoltage() != null) {
            product.setVoltage(updatedProduct.getVoltage());
        }
        if (updatedProduct.getWindSpeedLevels() != null) {
            product.setWindSpeedLevels(updatedProduct.getWindSpeedLevels());
        }
        if (updatedProduct.getAirflow() != null) {
            product.setAirflow(updatedProduct.getAirflow());
        }
        if (updatedProduct.getBladeMaterial() != null) {
            product.setBladeMaterial(updatedProduct.getBladeMaterial());
        }
        if (updatedProduct.getBodyMaterial() != null) {
            product.setBodyMaterial(updatedProduct.getBodyMaterial());
        }
        if (updatedProduct.getBladeCount() != null) {
            product.setBladeCount(updatedProduct.getBladeCount());
        }
        if (updatedProduct.getNoiseLevel() != null) {
            product.setNoiseLevel(updatedProduct.getNoiseLevel());
        }
        if (updatedProduct.getMotorSpeed() != null) {
            product.setMotorSpeed(updatedProduct.getMotorSpeed());
        }
        if (updatedProduct.getWeight() != null) {
            product.setWeight(updatedProduct.getWeight());
        }
        if (updatedProduct.getAdjustableHeight() != null) {
            product.setAdjustableHeight(updatedProduct.getAdjustableHeight());
        }
        
        // Cập nhật features
        if (updatedProduct.getRemoteControl() != null) {
            product.setRemoteControl(updatedProduct.getRemoteControl());
        }
        if (updatedProduct.getTimer() != null) {
            product.setTimer(updatedProduct.getTimer());
        }
        if (updatedProduct.getNaturalWindMode() != null) {
            product.setNaturalWindMode(updatedProduct.getNaturalWindMode());
        }
        if (updatedProduct.getSleepMode() != null) {
            product.setSleepMode(updatedProduct.getSleepMode());
        }
        if (updatedProduct.getOscillation() != null) {
            product.setOscillation(updatedProduct.getOscillation());
        }
        if (updatedProduct.getHeightAdjustable() != null) {
            product.setHeightAdjustable(updatedProduct.getHeightAdjustable());
        }
        if (updatedProduct.getAutoShutoff() != null) {
            product.setAutoShutoff(updatedProduct.getAutoShutoff());
        }
        if (updatedProduct.getTemperatureSensor() != null) {
            product.setTemperatureSensor(updatedProduct.getTemperatureSensor());
        }
        if (updatedProduct.getEnergySaving() != null) {
            product.setEnergySaving(updatedProduct.getEnergySaving());
        }
        
        // Cập nhật other information
        if (updatedProduct.getSafetyStandards() != null) {
            product.setSafetyStandards(updatedProduct.getSafetyStandards());
        }
        if (updatedProduct.getManufacturingYear() != null) {
            product.setManufacturingYear(updatedProduct.getManufacturingYear());
        }
        if (updatedProduct.getAccessories() != null) {
            product.setAccessories(updatedProduct.getAccessories());
        }
        if (updatedProduct.getEnergyRating() != null) {
            product.setEnergyRating(updatedProduct.getEnergyRating());
        }
        
        // fanType removed - no longer used
        
        if (updatedProduct.getColorDefault() != null) {
            product.setColorDefault(updatedProduct.getColorDefault());
        }
        
        if (updatedProduct.getWarrantyMonths() != null) {
            product.setWarrantyMonths(updatedProduct.getWarrantyMonths());
        }
        
        // Cập nhật quantity
        if (updatedProduct.getQuantity() != null) {
            product.setQuantity(updatedProduct.getQuantity());
        }
        
        // Cập nhật Image URL - Handle through ProductImage entity
        if (updatedProduct.getImageURL() != null && !updatedProduct.getImageURL().trim().isEmpty()) {
            // Xóa các ảnh cũ bằng cách clear images list (orphanRemoval sẽ tự động xóa)
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                System.out.println("ProductService: Deleting " + product.getImages().size() + " old ProductImage(s)");
                product.getImages().clear(); // orphanRemoval = true sẽ tự động xóa
            }
            
            // Tạo ảnh mới
            com.example.onlyfanshop_be.entity.ProductImage productImage = 
                    com.example.onlyfanshop_be.entity.ProductImage.builder()
                            .productId(product.getId().longValue())
                            .imageUrl(updatedProduct.getImageURL().trim())
                            .isMain(true) // Đặt làm ảnh chính
                            .sortOrder(0)
                            .build();
            
            // Set product reference để cascade hoạt động
            productImage.setProduct(product);
            
            // Tạo list images nếu chưa có
            if (product.getImages() == null) {
                product.setImages(new java.util.ArrayList<>());
            }
            
            // Add image vào product's images list
            product.getImages().add(productImage);
            
            System.out.println("ProductService: Added new ProductImage with URL: " + updatedProduct.getImageURL());
        }
        
        // Cập nhật Category (nếu có)
        if (updatedProduct.getCategoryID() != null) {
            product.setCategoryId(updatedProduct.getCategoryID());
        }

        // Cập nhật Brand (nếu có)
        if (updatedProduct.getBrandID() != null) {
            product.setBrandId(updatedProduct.getBrandID());
            // Nếu brand thay đổi, regenerate SKU
            Brand newBrand = brandRepository.findById(updatedProduct.getBrandID())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu có ID: " + updatedProduct.getBrandID()));
            String newSku = generateSku(newBrand.getId(), newBrand.getName());
            product.setSku(newSku);
        }
        
        // Cập nhật Colors (nếu có)
        if (updatedProduct.getColorIds() != null) {
            if (updatedProduct.getColorIds().isEmpty()) {
                // Nếu colorIds là empty array, xóa tất cả colors
                product.setColors(new java.util.ArrayList<>());
            } else {
                List<com.example.onlyfanshop_be.entity.Color> colors = colorRepository.findAllById(updatedProduct.getColorIds());
                if (colors.size() != updatedProduct.getColorIds().size()) {
                    throw new RuntimeException("Một hoặc nhiều màu sắc không tồn tại");
                }
                product.setColors(colors);
            }
        }
        
        // Cập nhật Warranty (nếu có)
        if (updatedProduct.getWarrantyId() != null) {
            if (updatedProduct.getWarrantyId() == 0) {
                // Nếu warrantyId là 0, xóa warranty
                product.setWarrantyId(null);
            } else {
                com.example.onlyfanshop_be.entity.Warranty warranty = warrantyRepository.findById(updatedProduct.getWarrantyId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bảo hành có ID: " + updatedProduct.getWarrantyId()));
                product.setWarrantyId(warranty.getId());
            }
        }
        
        product.setUpdatedAt(java.time.LocalDateTime.now());

        // Save product (nếu có update image, cascade sẽ tự động save ProductImage)
        Product savedProduct = productRepository.save(product);
        
        // Reload product để đảm bảo images được load trong EntityGraph
        savedProduct = productRepository.findById(savedProduct.getId().intValue())
                .orElse(savedProduct);

        // Trả về DTO với tất cả các field mới
        return buildProductDetailDTO(savedProduct);
    }

    @Override
    public void deleteProduct(int id) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            product.setStatus(com.example.onlyfanshop_be.enums.ProductStatus.inactive);
            productRepository.save(product);
        }else throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
    }
    @Override
    public void updateImage(int productId, String imageURL) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (!productOpt.isPresent()) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }
        
        Product product = productOpt.get();
        
        try {
            // Xóa các ảnh cũ bằng cách query và delete trực tiếp
            List<com.example.onlyfanshop_be.entity.ProductImage> existingImages = 
                    productImageRepository.findByProductId(product.getId());
            if (existingImages != null && !existingImages.isEmpty()) {
                System.out.println("ProductService: Deleting " + existingImages.size() + " old ProductImage(s)");
                productImageRepository.deleteAll(existingImages);
            }
            
            // Tạo ảnh mới nếu có imageURL
            if (imageURL != null && !imageURL.trim().isEmpty()) {
                // Tạo ProductImage entity
                com.example.onlyfanshop_be.entity.ProductImage productImage = 
                        com.example.onlyfanshop_be.entity.ProductImage.builder()
                                .productId(product.getId())
                                .imageUrl(imageURL.trim())
                                .isMain(true) // Đặt làm ảnh chính
                                .sortOrder(0)
                                .build();
                
                // Save trực tiếp vào database
                productImageRepository.save(productImage);
                
                System.out.println("ProductService: Updated ProductImage with URL: " + imageURL);
                System.out.println("ProductService: ProductImage saved with ID: " + productImage.getId() + ", ProductId: " + productImage.getProductId());
            }
        } catch (Exception e) {
            System.err.println("ProductService: Error updating ProductImage: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể cập nhật ảnh sản phẩm: " + e.getMessage(), e);
        }
    }

    @Override
    public ApiResponse<HomepageResponse> productList(
            String keyword, Integer categoryId, Integer brandId,
            Long minPrice, Long maxPrice, Integer bladeCount,
            Boolean remoteControl, Boolean oscillation, Boolean timer,
            Integer minPower, Integer maxPower,
            int page, int size, String sortBy, String order) {
        try {
            System.out.println("ProductService.productList - sortBy: " + sortBy);
            Sort.Direction direction = "DESC".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
            String actualSortField = mapSortField(sortBy);
            System.out.println("ProductService.productList - mapped sortBy: " + actualSortField);
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, actualSortField));

            Specification<Product> spec = (root, query, cb) -> 
                    cb.equal(root.get("status"), com.example.onlyfanshop_be.enums.ProductStatus.active);
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and((root, query, cb) ->
                        cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"));
            }

            if (categoryId != null && categoryId > 0) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("categoryId"), categoryId));
            }

            if (brandId != null && brandId > 0) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("brandId"), brandId));
            }

            // Filter theo giá
            if (minPrice != null) {
                spec = spec.and((root, query, cb) ->
                        cb.greaterThanOrEqualTo(root.get("basePrice"), java.math.BigDecimal.valueOf(minPrice)));
            }
            if (maxPrice != null) {
                spec = spec.and((root, query, cb) ->
                        cb.lessThanOrEqualTo(root.get("basePrice"), java.math.BigDecimal.valueOf(maxPrice)));
            }

            // Filter theo số cánh quạt
            if (bladeCount != null && bladeCount > 0) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("bladeCount"), bladeCount));
            }

            // Filter theo tiện ích
            if (remoteControl != null && remoteControl) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("remoteControl"), true));
            }
            if (oscillation != null && oscillation) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("oscillation"), true));
            }
            if (timer != null && timer) {
                spec = spec.and((root, query, cb) ->
                        cb.isNotNull(root.get("timer")));
            }

            // Filter theo công suất
            if (minPower != null) {
                spec = spec.and((root, query, cb) ->
                        cb.greaterThanOrEqualTo(root.get("powerWatt"), minPower));
            }
            if (maxPower != null) {
                spec = spec.and((root, query, cb) ->
                        cb.lessThanOrEqualTo(root.get("powerWatt"), maxPower));
            }

            Page<Product> productPage = productRepository.findAll(spec, pageable);
            List<Product> products = productPage.getContent();
            
            // Load all product images in batch to avoid N+1 query problem
            java.util.Map<Long, String> productImageMap = loadProductImagesBatch(products);

            List<ProductDTO> productDTOs = products.stream()
                    .map(p -> {
                        BrandDTO brandDTO = null;
                        if (p.getBrand() != null) {
                            brandDTO = BrandDTO.builder()
                                    .brandID(p.getBrand().getBrandID() == null ? null : p.getBrand().getBrandID().intValue())
                                    .name(p.getBrand().getBrandName())
                                    .imageURL(p.getBrand().getImageURL())
                                    .build();
                        }
                        
                        CategoryDTO categoryDTO = null;
                        if (p.getCategory() != null) {
                            categoryDTO = new CategoryDTO(
                                    p.getCategory().getCategoryID(),
                                    p.getCategory().getCategoryName()
                            );
                        }
                        
                        // Get image URL from map (already loaded in batch)
                        String imageURL = productImageMap.get(p.getId());
                        
                        return ProductDTO.builder()
                                .id(p.getProductID())
                                .productName(p.getProductName())
                                .price(p.getPrice())
                                .imageURL(imageURL)
                                .briefDescription(p.getBriefDescription())
                                .isActive(p.isActive())
                                .brand(brandDTO)
                                .category(categoryDTO)
                                .build();
                    })
                    .toList();
            java.math.BigDecimal maxPriceBD = productRepository.findMaxPrice();
            java.math.BigDecimal minPriceBD = productRepository.findMinPrice();
            Long maxPriceFilter = maxPriceBD != null ? maxPriceBD.longValue() : null;
            Long minPriceFilter = minPriceBD != null ? minPriceBD.longValue() : null;

            HomepageResponse.Filters filters = HomepageResponse.Filters.builder()
                    .selectedCategory(categoryId != null && categoryId > 0 ? categoryRepository.findById(categoryId).map(Category::getCategoryName).orElse("All") : "All")
                    .selectedBrand(brandId != null && brandId > 0 ? brandRepository.findById(brandId).map(Brand::getBrandName).orElse("All") : "All")
                    .sortOption(sortBy + "_" + order.toLowerCase())
                    .maxPrice(maxPriceFilter)
                    .minPrice(minPriceFilter)
                    .build();

            List<CategoryDTO> categories = categoryRepository.findAll().stream()
                    .map(c -> new CategoryDTO(c.getCategoryID(), c.getCategoryName()))
                    .toList();

            List<BrandDTO> brands = brandRepository.findAll().stream()
                    .map(b -> BrandDTO.builder()
                            .brandID(b.getBrandID() == null ? null : b.getBrandID().intValue())
                            .name(b.getBrandName())
                            .imageURL(b.getImageURL())
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
        } catch (Exception e) {
            System.err.println("Error in ProductService.productList: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi lấy danh sách sản phẩm: " + e.getMessage(), e);
        }
    }
    @Override
    public void updateActive(int productId, boolean active) {
        Optional<Product> product = productRepository.findById(productId);
        if(product.isPresent()) {
            product.get().setStatus(active ? 
                    com.example.onlyfanshop_be.enums.ProductStatus.active : 
                    com.example.onlyfanshop_be.enums.ProductStatus.inactive);
            productRepository.save(product.get());
        }else throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
    }

    @Override
    public void updateActiveByBrand(int brandID) {
        // Note: Brand no longer has isActive field in new schema
        // This method may need to be updated based on business requirements
        // For now, we keep products active
        // You may want to implement different logic based on your requirements
        productRepository.findByBrandId(brandID);
    }

    @Override
    public void updateActiveByCategory(int categoryID) {
        // Note: Category no longer has isActive field in new schema
        // This method may need to be updated based on business requirements
        // For now, we keep products active
        // You may want to implement different logic based on your requirements
        productRepository.findByCategoryId(categoryID);
    }
    
    // Helper method to map legacy field names to actual entity field names
    private String mapSortField(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            return "id"; // Default sort field
        }
        
        // Normalize to lowercase and trim
        String normalized = sortBy.toLowerCase().trim();
        
        // Map legacy field names to actual entity field names
        return switch (normalized) {
            case "productid", "product_id" -> "id";
            case "productname", "product_name" -> "name";
            case "price", "baseprice", "base_price" -> "basePrice";
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            case "slug" -> "slug";
            case "sku" -> "sku";
            case "status" -> "status";
            case "name" -> "name";
            case "id" -> "id";
            default -> {
                // If the field doesn't exist in Product entity, default to "id"
                // This prevents "Could not resolve attribute" errors
                System.out.println("Warning: Unknown sort field '" + sortBy + "', defaulting to 'id'");
                yield "id";
            }
        };
    }
    
    // Helper method to generate SKU from brand name and sequential number
    private String generateSku(Integer brandId, String brandName) {
        if (brandName == null || brandName.trim().isEmpty()) {
            throw new RuntimeException("Tên thương hiệu không được để trống");
        }
        
        // Đếm số lượng sản phẩm hiện có của brand này
        Long productCount = productRepository.countByBrandId(brandId);
        if (productCount == null) {
            productCount = 0L;
        }
        
        // Số thứ tự tiếp theo (bắt đầu từ 001)
        int nextSequence = productCount.intValue() + 1;
        
        // Format brand name: uppercase, thay khoảng trắng và ký tự đặc biệt bằng underscore
        String brandPrefix = brandName.toUpperCase()
                .trim()
                .replaceAll("[^A-Z0-9]", "_") // Thay tất cả ký tự không phải chữ và số bằng underscore
                .replaceAll("_+", "_") // Thay nhiều underscore liên tiếp bằng một underscore
                .replaceAll("^_|_$", ""); // Loại bỏ underscore ở đầu và cuối
        
        if (brandPrefix.isEmpty()) {
            brandPrefix = "BRAND";
        }
        
        // Format SKU: BRANDNAME_XXX (với XXX là số có 3 chữ số)
        return String.format("%s_%03d", brandPrefix, nextSequence);
    }
    
    // Helper method to generate slug from product name
    private String generateSlug(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return "product-" + System.currentTimeMillis();
        }
        
        // Convert to lowercase, remove diacritics, replace spaces with hyphens
        String baseSlug = productName.toLowerCase()
                .trim()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single hyphen
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
        
        if (baseSlug.isEmpty()) {
            baseSlug = "product";
        }
        
        // Check if slug already exists, if yes, append number
        List<Product> allProducts = productRepository.findAll();
        int counter = 0;
        String slug;
        
        do {
            if (counter == 0) {
                slug = baseSlug;
            } else {
                slug = baseSlug + "-" + counter;
            }
            final String currentSlug = slug; // Final variable for lambda
            boolean slugExists = allProducts.stream()
                    .anyMatch(p -> p.getSlug() != null && p.getSlug().equals(currentSlug));
            if (!slugExists) {
                break;
            }
            counter++;
        } while (true);
        
        return slug;
    }
    
    // Helper method to load product images in batch (avoids N+1 query problem)
    private java.util.Map<Long, String> loadProductImagesBatch(List<Product> products) {
        java.util.Map<Long, String> imageMap = new java.util.HashMap<>();
        
        if (products == null || products.isEmpty()) {
            return imageMap;
        }
        
        // Get all product IDs
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        
        if (productIds.isEmpty()) {
            return imageMap;
        }
        
        try {
            // Load all images for all products in one query (much more efficient)
            List<com.example.onlyfanshop_be.entity.ProductImage> allImages = 
                    productImageRepository.findByProductIdIn(productIds);
            
            // Group images by productId and get main image for each product
            java.util.Map<Long, java.util.List<com.example.onlyfanshop_be.entity.ProductImage>> imagesByProduct = 
                    allImages.stream()
                            .collect(java.util.stream.Collectors.groupingBy(
                                    com.example.onlyfanshop_be.entity.ProductImage::getProductId));
            
            // Extract main image (or first image) for each product
            for (java.util.Map.Entry<Long, java.util.List<com.example.onlyfanshop_be.entity.ProductImage>> entry : 
                    imagesByProduct.entrySet()) {
                Long productId = entry.getKey();
                java.util.List<com.example.onlyfanshop_be.entity.ProductImage> images = entry.getValue();
                
                if (images != null && !images.isEmpty()) {
                    // Get main image if available, otherwise get first image
                    String imageURL = images.stream()
                            .filter(com.example.onlyfanshop_be.entity.ProductImage::getIsMain)
                            .map(com.example.onlyfanshop_be.entity.ProductImage::getImageUrl)
                            .findFirst()
                            .orElse(images.get(0).getImageUrl());
                    imageMap.put(productId, imageURL);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load images in batch: " + e.getMessage());
            e.printStackTrace();
        }
        
        return imageMap;
    }
    
    // Helper method to get product image URL (handles lazy loading)
    private String getProductImageURL(Product product, List<com.example.onlyfanshop_be.entity.ProductImage> images) {
        if (images != null && !images.isEmpty()) {
            return images.stream()
                    .filter(com.example.onlyfanshop_be.entity.ProductImage::getIsMain)
                    .map(com.example.onlyfanshop_be.entity.ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(images.get(0).getImageUrl());
        }
        // Fallback: try to get from product's getImageURL() method
        // This will trigger lazy loading if images haven't been loaded
        try {
            return product.getImageURL();
        } catch (Exception e) {
            // If lazy loading fails, return null
            return null;
        }
    }
    
    // Helper method to build ProductDetailDTO from Product entity
    private ProductDetailDTO buildProductDetailDTO(Product product) {
        // Load colors if needed (they are lazy loaded)
        // Accessing colors will trigger lazy loading if not already loaded
        List<com.example.onlyfanshop_be.entity.Color> colors = null;
        try {
            colors = product.getColors();
            // Force initialization if it's a lazy collection
            if (colors != null) {
                // Access size to trigger lazy loading
                colors.size();
            }
        } catch (Exception e) {
            // If lazy loading fails, colors will be null
            System.err.println("Warning: Could not load colors for product " + product.getId() + ": " + e.getMessage());
        }
        
        // Load images separately to avoid MultipleBagFetchException
        // Accessing images will trigger lazy loading if not already loaded
        List<com.example.onlyfanshop_be.entity.ProductImage> images = null;
        try {
            images = product.getImages();
            // Force initialization if it's a lazy collection
            if (images != null) {
                // Access size to trigger lazy loading
                images.size();
            }
        } catch (Exception e) {
            // If lazy loading fails, images will be null
            System.err.println("Warning: Could not load images for product " + product.getId() + ": " + e.getMessage());
        }
        
        com.example.onlyfanshop_be.entity.Warranty warranty = product.getWarranty();
        
        return ProductDetailDTO.builder()
                .id(product.getProductID())
                .productName(product.getProductName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .briefDescription(product.getBriefDescription())
                .fullDescription(product.getFullDescription())
                .technicalSpecifications(buildTechnicalSpecifications(product))
                .price(product.getPrice())
                .imageURL(getProductImageURL(product, images))
                .powerWatt(product.getPowerWatt())
                .bladeDiameterCm(product.getBladeDiameterCm())
                // Technical specifications
                .voltage(product.getVoltage())
                .windSpeedLevels(product.getWindSpeedLevels())
                .airflow(product.getAirflow())
                .bladeMaterial(product.getBladeMaterial())
                .bodyMaterial(product.getBodyMaterial())
                .bladeCount(product.getBladeCount())
                .noiseLevel(product.getNoiseLevel())
                .motorSpeed(product.getMotorSpeed())
                .weight(product.getWeight())
                .adjustableHeight(product.getAdjustableHeight())
                // Features
                .remoteControl(product.getRemoteControl())
                .timer(product.getTimer())
                .naturalWindMode(product.getNaturalWindMode())
                .sleepMode(product.getSleepMode())
                .oscillation(product.getOscillation())
                .heightAdjustable(product.getHeightAdjustable())
                .autoShutoff(product.getAutoShutoff())
                .temperatureSensor(product.getTemperatureSensor())
                .energySaving(product.getEnergySaving())
                // Other information
                .safetyStandards(product.getSafetyStandards())
                .manufacturingYear(product.getManufacturingYear())
                .accessories(product.getAccessories())
                .energyRating(product.getEnergyRating())
                // Legacy fields
                .colorDefault(product.getColorDefault()) // Legacy field
                .warrantyMonths(product.getWarrantyMonths()) // Legacy field
                .quantity(product.getQuantity()) // Số lượng sản phẩm
                .colors(colors) // New relationship
                .warranty(warranty) // New relationship
                .brand(product.getBrand() != null
                        ? new BrandDTO(
                        product.getBrand().getBrandID(),
                        product.getBrand().getBrandName(),
                        null, // Country field removed from Brand
                        product.getBrand().getDescription(),
                        product.getBrand().getImageURL(),
                        product.getBrand().isActive()
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
    
    // Helper method to build technical specifications string
    private String buildTechnicalSpecifications(Product product) {
        StringBuilder specs = new StringBuilder();
        if (product.getPowerWatt() != null) {
            specs.append("Công suất: ").append(product.getPowerWatt()).append("W\n");
        }
        if (product.getBladeDiameterCm() != null) {
            specs.append("Đường kính cánh quạt: ").append(product.getBladeDiameterCm()).append("cm\n");
        }
        if (product.getColorDefault() != null) {
            specs.append("Màu sắc: ").append(product.getColorDefault()).append("\n");
        }
        if (product.getWarrantyMonths() != null) {
            specs.append("Bảo hành: ").append(product.getWarrantyMonths()).append(" tháng\n");
        }
        return specs.length() > 0 ? specs.toString().trim() : null;
    }
}
