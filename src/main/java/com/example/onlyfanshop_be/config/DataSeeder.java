package com.example.onlyfanshop_be.config;

import com.example.onlyfanshop_be.entity.Brand;
import com.example.onlyfanshop_be.entity.Category;
import com.example.onlyfanshop_be.repository.BrandRepository;
import com.example.onlyfanshop_be.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedBrands();
        seedCategories();
        log.info("Data seeding completed successfully!");
    }

    private void seedBrands() {
        log.info("Starting to seed Brand data...");
        
        // Get all enum values
        com.example.onlyfanshop_be.enums.Brand[] brandEnums = com.example.onlyfanshop_be.enums.Brand.values();
        
        for (com.example.onlyfanshop_be.enums.Brand brandEnum : brandEnums) {
            // Check if brand already exists
            if (!brandRepository.existsByBrandName(brandEnum.getDisplayName())) {
                Brand brand = Brand.builder()
                        .brandName(brandEnum.getDisplayName())
                        .country(extractCountryFromDescription(brandEnum.getDescription()))
                        .description(brandEnum.getDescription())
                        .build();
                
                brandRepository.save(brand);
                log.info("Saved brand: {}", brandEnum.getDisplayName());
            } else {
                log.debug("Brand already exists: {}", brandEnum.getDisplayName());
            }
        }
        
        log.info("Completed seeding Brand data. Total brands in database: {}", brandRepository.count());
    }

    private void seedCategories() {
        log.info("Starting to seed Category data...");
        
        // Get all enum values
        com.example.onlyfanshop_be.enums.Category[] categoryEnums = com.example.onlyfanshop_be.enums.Category.values();
        
        for (com.example.onlyfanshop_be.enums.Category categoryEnum : categoryEnums) {
            // Check if category already exists
            if (!categoryRepository.existsByCategoryName(categoryEnum.getDisplayName())) {
                Category category = Category.builder()
                        .categoryName(categoryEnum.getDisplayName())
                        .build();
                
                categoryRepository.save(category);
                log.info("Saved category: {}", categoryEnum.getDisplayName());
            } else {
                log.debug("Category already exists: {}", categoryEnum.getDisplayName());
            }
        }
        
        log.info("Completed seeding Category data. Total categories in database: {}", categoryRepository.count());
    }

    private String extractCountryFromDescription(String description) {
        // Simple logic to extract country from description
        if (description.contains("Việt Nam")) {
            return "Việt Nam";
        } else if (description.contains("Nhật Bản") || description.contains("Nhật")) {
            return "Nhật Bản";
        } else if (description.contains("Hàn Quốc")) {
            return "Hàn Quốc";
        } else if (description.contains("Trung Quốc")) {
            return "Trung Quốc";
        } else if (description.contains("Anh")) {
            return "Anh";
        } else if (description.contains("Mỹ")) {
            return "Mỹ";
        } else {
            return "Khác";
        }
    }
}