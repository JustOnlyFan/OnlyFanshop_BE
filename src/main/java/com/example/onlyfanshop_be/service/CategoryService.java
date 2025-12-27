package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Category;
import com.example.onlyfanshop_be.enums.CategoryType;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên danh mục không được để trống");
        }

        String categoryName = category.getName().trim();
        if (categoryRepository.existsByName(categoryName)) {
            throw new RuntimeException("Danh mục với tên '" + categoryName + "' đã tồn tại");
        }

        String slug = category.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlugForCategory(categoryName);
        }

        Category c = new Category();
        c.setName(categoryName);
        c.setSlug(slug);
        c.setDescription(category.getDescription());
        c.setParentId(category.getParentId());

        return categoryRepository.save(c);
    }

    public Category updateCategory(Integer id, Category updatedCategory) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục có ID: " + id));

        if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
            String slug = generateSlugForCategory(category.getName());
            category.setSlug(slug);
        }

        if (updatedCategory.getName() != null && !updatedCategory.getName().trim().isEmpty()) {
            String oldName = category.getName();
            String newName = updatedCategory.getName().trim();

            if (!oldName.equals(newName) && categoryRepository.existsByName(newName)) {
                throw new RuntimeException("Danh mục với tên '" + newName + "' đã tồn tại");
            }

            if (!oldName.equals(newName)) {
                String newSlug = generateSlugForCategory(newName);
                category.setSlug(newSlug);
            }
            
            category.setName(newName);
        }

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

        if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
            String slug = generateSlugForCategory(category.getName());
            category.setSlug(slug);
        }

        categoryRepository.save(category);
        productService.updateActiveByCategory(id);
        return category;
    }

    public void deleteCategory(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        if (categoryRepository.existsByParentId(id)) {
            throw new AppException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }
        
        categoryRepository.deleteById(id);
    }

    public List<Category> getCategoriesByType(CategoryType categoryType) {
        if (categoryType == null) {
            throw new AppException(ErrorCode.CATEGORY_TYPE_REQUIRED);
        }
        return categoryRepository.findByCategoryTypeOrderByDisplayOrderAsc(categoryType);
    }

    public List<Category> getCategoryTree(CategoryType categoryType) {
        if (categoryType == null) {
            throw new AppException(ErrorCode.CATEGORY_TYPE_REQUIRED);
        }

        List<Category> rootCategories = categoryRepository.findRootCategoriesWithChildren(categoryType);

        for (Category root : rootCategories) {
            loadChildrenRecursively(root);
        }
        
        return rootCategories;
    }

    private void loadChildrenRecursively(Category category) {
        if (category == null || category.getId() == null) {
            return;
        }
        
        List<Category> children = categoryRepository.findByParentIdOrderByDisplayOrderAsc(category.getId());
        category.setChildren(children);
        
        for (Category child : children) {
            child.setParent(category);
            loadChildrenRecursively(child);
        }
    }

    public List<Category> getChildCategories(Integer parentId) {
        if (parentId == null) {
            throw new AppException(ErrorCode.PARENT_NOT_FOUND);
        }
        return categoryRepository.findByParentIdOrderByDisplayOrderAsc(parentId);
    }

    public int getCategoryDepth(Integer categoryId) {
        if (categoryId == null) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        
        return calculateDepth(category);
    }

    private int calculateDepth(Category category) {
        int depth = 1;
        Integer parentId = category.getParentId();
        
        while (parentId != null) {
            depth++;
            Category parent = categoryRepository.findById(parentId).orElse(null);
            if (parent == null) {
                break;
            }
            parentId = parent.getParentId();
        }
        
        return depth;
    }

    public boolean canHaveChildren(Integer categoryId) {
        int depth = getCategoryDepth(categoryId);
        return depth < 3;
    }

    public void validateParentChildType(Integer parentId, CategoryType childType) {
        if (parentId == null) {
            return; // Root category, no validation needed
        }
        
        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new AppException(ErrorCode.PARENT_NOT_FOUND));
        
        if (parent.getCategoryType() != childType) {
            throw new AppException(ErrorCode.CATEGORY_TYPE_MISMATCH);
        }
        
        // Also validate depth
        int parentDepth = calculateDepth(parent);
        if (parentDepth >= 3) {
            throw new AppException(ErrorCode.CATEGORY_MAX_DEPTH_EXCEEDED);
        }
    }

    @Transactional
    public Category createCategoryWithValidation(Category category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên danh mục không được để trống");
        }
        
        String categoryName = category.getName().trim();
        if (categoryRepository.existsByName(categoryName)) {
            throw new AppException(ErrorCode.CATEGORY_NAME_EXISTS);
        }

        if (category.getCategoryType() == null) {
            throw new AppException(ErrorCode.CATEGORY_TYPE_REQUIRED);
        }

        if (category.getParentId() != null) {
            validateParentChildType(category.getParentId(), category.getCategoryType());
        }

        String slug = category.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlugForCategory(categoryName);
        }

        Category newCategory = Category.builder()
                .name(categoryName)
                .slug(slug)
                .categoryType(category.getCategoryType())
                .parentId(category.getParentId())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .displayOrder(category.getDisplayOrder() != null ? category.getDisplayOrder() : 0)
                .isActive(category.getIsActive() != null ? category.getIsActive() : true)
                .build();
        
        return categoryRepository.save(newCategory);
    }

    @Transactional
    public Category updateCategoryWithValidation(Integer id, Category updatedCategory) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (updatedCategory.getName() != null && !updatedCategory.getName().trim().isEmpty()) {
            String oldName = category.getName();
            String newName = updatedCategory.getName().trim();
            
            if (!oldName.equals(newName)) {
                if (categoryRepository.existsByName(newName)) {
                    throw new AppException(ErrorCode.CATEGORY_NAME_EXISTS);
                }
                category.setName(newName);
                category.setSlug(generateSlugForCategory(newName));
            }
        }

        if (updatedCategory.getCategoryType() != null && 
            updatedCategory.getCategoryType() != category.getCategoryType()) {

            if (categoryRepository.existsByParentId(id)) {
                throw new RuntimeException("Không thể thay đổi loại danh mục khi có danh mục con");
            }

            if (category.getParentId() != null) {
                validateParentChildType(category.getParentId(), updatedCategory.getCategoryType());
            }
            
            category.setCategoryType(updatedCategory.getCategoryType());
        }

        if (updatedCategory.getDescription() != null) {
            category.setDescription(updatedCategory.getDescription());
        }
        if (updatedCategory.getIconUrl() != null) {
            category.setIconUrl(updatedCategory.getIconUrl());
        }
        if (updatedCategory.getDisplayOrder() != null) {
            category.setDisplayOrder(updatedCategory.getDisplayOrder());
        }
        if (updatedCategory.getIsActive() != null) {
            category.setIsActive(updatedCategory.getIsActive());
        }
        
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategoryWithChildrenCheck(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        
        // Check if category has children
        if (categoryRepository.existsByParentId(id)) {
            throw new AppException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }
        
        categoryRepository.deleteById(id);
    }

    public List<Integer> getAllDescendantCategoryIds(Integer categoryId) {
        List<Integer> result = new ArrayList<>();
        result.add(categoryId);
        
        collectDescendantIds(categoryId, result);
        
        return result;
    }

    private void collectDescendantIds(Integer parentId, List<Integer> result) {
        List<Category> children = categoryRepository.findByParentId(parentId);
        for (Category child : children) {
            result.add(child.getId());
            collectDescendantIds(child.getId(), result);
        }
    }

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
