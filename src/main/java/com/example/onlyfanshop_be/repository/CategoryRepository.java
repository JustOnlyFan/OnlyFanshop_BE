package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Category;
import com.example.onlyfanshop_be.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    boolean existsByName(String name);

    List<Category> findByCategoryTypeOrderByDisplayOrderAsc(CategoryType categoryType);

    List<Category> findByParentId(Integer parentId);

    List<Category> findByParentIdOrderByDisplayOrderAsc(Integer parentId);

    boolean existsByParentId(Integer parentId);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.categoryType = :categoryType ORDER BY c.parentId NULLS FIRST, c.displayOrder ASC")
    List<Category> findCategoryTreeByType(@Param("categoryType") CategoryType categoryType);

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parentId IS NULL AND c.categoryType = :categoryType ORDER BY c.displayOrder ASC")
    List<Category> findRootCategoriesWithChildren(@Param("categoryType") CategoryType categoryType);
}

