package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Category;
import com.example.onlyfanshop_be.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductService productService;

    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        List<Category> categoriesToSave = new ArrayList<>(); // Collect categories that need slug fix
        
        // Đảm bảo tất cả category đều có slug (fix cho các category cũ không có slug)
        for (Category category : categories) {
            if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
                String slug = generateSlugForCategory(category.getName());
                category.setSlug(slug);
                categoriesToSave.add(category); // Collect để save sau
            }
        }
        
        // Save tất cả categories cần fix slug trong một batch
        if (!categoriesToSave.isEmpty()) {
            categoryRepository.saveAll(categoriesToSave);
        }
        
        return categories;
    }

    public Category getCategoryById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục có ID: " + id));
        
        // Đảm bảo category có slug (fix cho các category cũ không có slug)
        if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
            String slug = generateSlugForCategory(category.getName());
            category.setSlug(slug);
            categoryRepository.save(category); // Lưu lại để fix vĩnh viễn
        }
        
        return category;
    }

    public Category createCategory(Category category) {
        // 1️⃣ Validate name
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên danh mục không được để trống");
        }
        
        // 2️⃣ Kiểm tra name đã tồn tại chưa
        String categoryName = category.getName().trim();
        if (categoryRepository.existsByName(categoryName)) {
            throw new RuntimeException("Danh mục với tên '" + categoryName + "' đã tồn tại");
        }
        
        // 3️⃣ Generate slug từ name nếu không được cung cấp
        String slug = category.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlugForCategory(categoryName);
        }
        
        // 4️⃣ Tạo Category entity
        Category c = new Category();
        c.setName(categoryName);
        c.setSlug(slug);
        c.setDescription(category.getDescription());
        c.setParentId(category.getParentId());
        
        // 5️⃣ Lưu vào DB
        return categoryRepository.save(c);
    }

    public Category updateCategory(Integer id, Category updatedCategory) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục có ID: " + id));

        // 0️⃣ Đảm bảo category hiện tại có slug (fix cho các category cũ không có slug)
        if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
            String slug = generateSlugForCategory(category.getName());
            category.setSlug(slug);
        }

        // 1️⃣ Validate và update name
        if (updatedCategory.getName() != null && !updatedCategory.getName().trim().isEmpty()) {
            String oldName = category.getName();
            String newName = updatedCategory.getName().trim();
            
            // Kiểm tra nếu name thay đổi và name mới đã tồn tại (trừ chính nó)
            if (!oldName.equals(newName) && categoryRepository.existsByName(newName)) {
                throw new RuntimeException("Danh mục với tên '" + newName + "' đã tồn tại");
            }
            
            // 2️⃣ Generate slug mới nếu name thay đổi
            if (!oldName.equals(newName)) {
                String newSlug = generateSlugForCategory(newName);
                category.setSlug(newSlug);
            }
            
            category.setName(newName);
        }
        
        // 3️⃣ Update các field khác
        if (updatedCategory.getDescription() != null) {
            category.setDescription(updatedCategory.getDescription());
        }
        
        if (updatedCategory.getParentId() != null) {
            category.setParentId(updatedCategory.getParentId());
        }

        return categoryRepository.save(category);
    }
    
    public Category toggleActive(Integer id, boolean active) {
        Category category = getCategoryById(id);
        
        // Đảm bảo category có slug (fix cho các category cũ không có slug)
        if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
            String slug = generateSlugForCategory(category.getName());
            category.setSlug(slug);
        }
        
        // Note: Category entity doesn't have active field - always active in new schema
        // This method is kept for backward compatibility but doesn't modify category status
        categoryRepository.save(category);
        productService.updateActiveByCategory(id);
        return category;
    }

    public void deleteCategory(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy danh mục có ID: " + id);
        }
        categoryRepository.deleteById(id);
    }
    
    // Helper method to generate slug from category name
    private String generateSlugForCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return "category-" + System.currentTimeMillis();
        }
        
        // Convert to lowercase, remove diacritics, replace spaces with hyphens
        String baseSlug = categoryName.toLowerCase()
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
            baseSlug = "category";
        }
        
        // Check if slug already exists, if yes, append number
        List<Category> allCategories = categoryRepository.findAll();
        int counter = 0;
        String slug;
        
        do {
            if (counter == 0) {
                slug = baseSlug;
            } else {
                slug = baseSlug + "-" + counter;
            }
            final String currentSlug = slug; // Final variable for lambda
            boolean slugExists = allCategories.stream()
                    .anyMatch(c -> c.getSlug() != null && c.getSlug().equals(currentSlug));
            if (!slugExists) {
                break;
            }
            counter++;
        } while (true);
        
        return slug;
    }
}
