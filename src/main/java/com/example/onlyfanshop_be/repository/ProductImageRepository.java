package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductId(Long productId);
    List<ProductImage> findByProductIdIn(List<Long> productIds);
    void deleteByProductId(Long productId);
    
    // OPTIMIZATION: Query only main images for homepage (faster)
    @Query("SELECT pi FROM ProductImage pi WHERE pi.productId IN :productIds AND pi.isMain = true")
    List<ProductImage> findMainImagesByProductIdIn(List<Long> productIds);
    
    // OPTIMIZATION: Get main image URL directly (even faster - only one field)
    @Query("SELECT pi.productId, pi.imageUrl FROM ProductImage pi WHERE pi.productId IN :productIds AND pi.isMain = true")
    List<Object[]> findMainImageUrlsByProductIdIn(List<Long> productIds);
}

