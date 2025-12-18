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
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        
        // Check if category has children - prevent deletion if it does
        if (categoryRepository.existsByParentId(id)) {
            throw new AppException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }
        
        categoryRepository.deleteById(id);
    }
    
    // ==================== NEW METHODS FOR EXPANDED CATEGORY SYSTEM ====================

    /**
     * Get all categories by type.
     * @param categoryType the category type to filter by
     * @return list of categories with the specified type, ordered by display order
     */
    public List<Category> getCategoriesByType(CategoryType categoryType) {
        if (categoryType == null) {
            throw new AppException(ErrorCode.CATEGORY_TYPE_REQUIRED);
        }
        return categoryRepository.findByCategoryTypeOrderByDisplayOrderAsc(categoryType);
    }

    /**
     * Get category tree by type (root categories with children loaded).
     * @param categoryType the category type to filter by
     * @return list of root categories with their children hierarchy
     */
    public List<Category> getCategoryTree(CategoryType categoryType) {
        if (categoryType == null) {
            throw new AppException(ErrorCode.CATEGORY_TYPE_REQUIRED);
        }
        
        // Get root categories with children eagerly loaded
        List<Category> rootCategories = categoryRepository.findRootCategoriesWithChildren(categoryType);
        
        // Build the full tree by loading children recursively
        for (Category root : rootCategories) {
            loadChildrenRecursively(root);
        }
        
        return rootCategories;
    }

    /**
     * Recursively load children for a category.
     */
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

    /**
     * Get child categories by parent ID.
     * @param parentId the parent category ID
     * @return list of child categories
     */
    public List<Category> getChildCategories(Integer parentId) {
        if (parentId == null) {
            throw new AppException(ErrorCode.PARENT_NOT_FOUND);
        }
        return categoryRepository.findByParentIdOrderByDisplayOrderAsc(parentId);
    }

    /**
     * Calculate the depth of a category in the hierarchy.
     * Root categories have depth 1.
     * @param categoryId the category ID
     * @return the depth level of the category
     */
    public int getCategoryDepth(Integer categoryId) {
        if (categoryId == null) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        
        return calculateDepth(category);
    }

    /**
     * Calculate depth by traversing up the parent chain.
     */
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

    /**
     * Validate that a category can have children (depth < 3).
     * @param categoryId the category ID to check
     * @return true if the category can have children
     */
    public boolean canHaveChildren(Integer categoryId) {
        int depth = getCategoryDepth(categoryId);
        return depth < 3;
    }

    /**
     * Validate parent-child category type consistency.
     * @param parentId the parent category ID
     * @param childType the category type of the child
     * @throws AppException if types don't match
     */
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

    /**
     * Create a category with full validation.
     * @param category the category to create
     * @return the created category
     */
    @Transactional
    public Category createCategoryWithValidation(Category category) {
        // 1. Validate name
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên danh mục không được để trống");
        }
        
        String categoryName = category.getName().trim();
        if (categoryRepository.existsByName(categoryName)) {
            throw new AppException(ErrorCode.CATEGORY_NAME_EXISTS);
        }
        
        // 2. Validate category type
        if (category.getCategoryType() == null) {
            throw new AppException(ErrorCode.CATEGORY_TYPE_REQUIRED);
        }
        
        // 3. Validate parent-child relationship
        if (category.getParentId() != null) {
            validateParentChildType(category.getParentId(), category.getCategoryType());
        }
        
        // 4. Generate slug
        String slug = category.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlugForCategory(categoryName);
        }
        
        // 5. Create and save
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

    /**
     * Update a category with full validation.
     * @param id the category ID
     * @param updatedCategory the updated category data
     * @return the updated category
     */
    @Transactional
    public Category updateCategoryWithValidation(Integer id, Category updatedCategory) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        
        // Update name and regenerate slug if name changed
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
        
        // Update category type (only if no children and no parent type conflict)
        if (updatedCategory.getCategoryType() != null && 
            updatedCategory.getCategoryType() != category.getCategoryType()) {
            
            // Check if has children
            if (categoryRepository.existsByParentId(id)) {
                throw new RuntimeException("Không thể thay đổi loại danh mục khi có danh mục con");
            }
            
            // Check parent type consistency
            if (category.getParentId() != null) {
                validateParentChildType(category.getParentId(), updatedCategory.getCategoryType());
            }
            
            category.setCategoryType(updatedCategory.getCategoryType());
        }
        
        // Update other fields
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

    /**
     * Delete a category with children check.
     * @param id the category ID to delete
     * @throws AppException if category has children
     */
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

    /**
     * Get all descendant category IDs for a given category (including itself).
     * Useful for filtering products by category including subcategories.
     * @param categoryId the root category ID
     * @return list of all descendant category IDs
     */
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

    // ==================== HELPER METHODS ====================

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

    /**
     * Generate slug from category name (public method for testing).
     * @param categoryName the category name
     * @return the generated slug
     */
    public String generateSlug(String categoryName) {
        return generateSlugForCategory(categoryName);
    }
}
