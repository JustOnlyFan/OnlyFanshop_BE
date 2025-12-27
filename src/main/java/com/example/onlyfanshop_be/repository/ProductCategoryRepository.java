package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.ProductCategory;
import com.example.onlyfanshop_be.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findByProductId(Long productId);

    @Query("SELECT pc FROM ProductCategory pc JOIN FETCH pc.category WHERE pc.productId = :productId")
    List<ProductCategory> findByProductIdWithCategory(@Param("productId") Long productId);

    @Query("SELECT pc FROM ProductCategory pc JOIN FETCH pc.category c WHERE pc.productId = :productId AND c.categoryType = :categoryType")
    List<ProductCategory> findByProductIdAndCategoryTypeWithCategory(@Param("productId") Long productId, @Param("categoryType") CategoryType categoryType);

    @Query("SELECT CASE WHEN COUNT(pc) > 0 THEN true ELSE false END FROM ProductCategory pc JOIN pc.category c WHERE pc.productId = :productId AND c.categoryType = :categoryType")
    boolean existsByProductIdAndCategoryType(@Param("productId") Long productId, @Param("categoryType") CategoryType categoryType);

    boolean existsByProductIdAndCategoryId(Long productId, Integer categoryId);

    Optional<ProductCategory> findByProductIdAndCategoryId(Long productId, Integer categoryId);

    void deleteByProductId(Long productId);

    void deleteByProductIdAndCategoryId(Long productId, Integer categoryId);

    List<ProductCategory> findByCategoryId(Integer categoryId);

    Optional<ProductCategory> findByProductIdAndIsPrimaryTrue(Long productId);

    long countByProductId(Long productId);

    @Query("SELECT DISTINCT pc.productId FROM ProductCategory pc JOIN pc.category c WHERE c.categoryType = :categoryType")
    List<Long> findProductIdsByCategoryType(@Param("categoryType") CategoryType categoryType);

    @Query("SELECT DISTINCT pc.productId FROM ProductCategory pc WHERE pc.categoryId IN :categoryIds")
    List<Long> findProductIdsByCategoryIds(@Param("categoryIds") List<Integer> categoryIds);
}
