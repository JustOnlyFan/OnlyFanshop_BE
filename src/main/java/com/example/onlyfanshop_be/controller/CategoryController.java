package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.CategoryDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Category;
import com.example.onlyfanshop_be.enums.CategoryType;
import com.example.onlyfanshop_be.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;


    @GetMapping("/public")
    public List<Category> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public Category getCategoryById(@PathVariable Integer id) {
        return categoryService.getCategoryById(id);
    }

    @PostMapping("/create")
    public Category createCategory(@RequestBody Category category) {
        return categoryService.createCategory(category);
    }

    @PutMapping("/{id}")
    public Category updateCategory(@PathVariable Integer id, @RequestBody Category category) {
        return categoryService.updateCategory(id, category);
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("switchActive/{id}")
    public Category toggleActiveCategory(@PathVariable Integer id, @RequestParam boolean active) {
        return categoryService.toggleActive(id, active);
    }

    @GetMapping("/public/type/{type}")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getCategoriesByType(@PathVariable CategoryType type) {
        try {
            List<Category> categories = categoryService.getCategoriesByType(type);
            List<CategoryDTO> categoryDTOs = categories.stream()
                    .map(CategoryDTO::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.<List<CategoryDTO>>builder()
                    .statusCode(200)
                    .message("Lấy danh mục theo loại thành công")
                    .data(categoryDTOs)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<CategoryDTO>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/public/tree/{type}")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getCategoryTree(@PathVariable CategoryType type) {
        try {
            List<Category> rootCategories = categoryService.getCategoryTree(type);
            List<CategoryDTO> categoryDTOs = rootCategories.stream()
                    .map(CategoryDTO::fromEntityWithChildren)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.<List<CategoryDTO>>builder()
                    .statusCode(200)
                    .message("Lấy cây danh mục thành công")
                    .data(categoryDTOs)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<CategoryDTO>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/public/types")
    public ResponseEntity<ApiResponse<CategoryType[]>> getAllCategoryTypes() {
        return ResponseEntity.ok(ApiResponse.<CategoryType[]>builder()
                .statusCode(200)
                .message("Lấy danh sách loại danh mục thành công")
                .data(CategoryType.values())
                .build());
    }

    @GetMapping("/public/children/{parentId}")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getChildCategories(@PathVariable Integer parentId) {
        try {
            List<Category> children = categoryService.getChildCategories(parentId);
            List<CategoryDTO> categoryDTOs = children.stream()
                    .map(CategoryDTO::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.<List<CategoryDTO>>builder()
                    .statusCode(200)
                    .message("Lấy danh mục con thành công")
                    .data(categoryDTOs)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<CategoryDTO>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{id}/depth")
    public ResponseEntity<ApiResponse<Integer>> getCategoryDepth(@PathVariable Integer id) {
        try {
            int depth = categoryService.getCategoryDepth(id);
            return ResponseEntity.ok(ApiResponse.<Integer>builder()
                    .statusCode(200)
                    .message("Lấy độ sâu danh mục thành công")
                    .data(depth)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Integer>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategoryWithValidation(@RequestBody CategoryDTO categoryDTO) {
        try {
            Category category = categoryDTO.toEntity();
            Category created = categoryService.createCategoryWithValidation(category);
            CategoryDTO result = CategoryDTO.fromEntity(created);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<CategoryDTO>builder()
                            .statusCode(201)
                            .message("Tạo danh mục thành công")
                            .data(result)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<CategoryDTO>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategoryWithValidation(
            @PathVariable Integer id, 
            @RequestBody CategoryDTO categoryDTO) {
        try {
            Category category = categoryDTO.toEntity();
            Category updated = categoryService.updateCategoryWithValidation(id, category);
            CategoryDTO result = CategoryDTO.fromEntity(updated);
            
            return ResponseEntity.ok(ApiResponse.<CategoryDTO>builder()
                    .statusCode(200)
                    .message("Cập nhật danh mục thành công")
                    .data(result)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<CategoryDTO>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategoryWithChildrenCheck(@PathVariable Integer id) {
        try {
            categoryService.deleteCategoryWithChildrenCheck(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .statusCode(200)
                    .message("Xóa danh mục thành công")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Void>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/public/dto/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategoryDTOById(@PathVariable Integer id) {
        try {
            Category category = categoryService.getCategoryById(id);
            CategoryDTO categoryDTO = CategoryDTO.fromEntity(category);
            
            return ResponseEntity.ok(ApiResponse.<CategoryDTO>builder()
                    .statusCode(200)
                    .message("Lấy danh mục thành công")
                    .data(categoryDTO)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<CategoryDTO>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }
}
