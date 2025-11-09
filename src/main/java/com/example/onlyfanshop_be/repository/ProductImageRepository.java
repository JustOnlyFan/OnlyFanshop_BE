package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductId(Long productId);
    List<ProductImage> findByProductIdIn(List<Long> productIds);
    void deleteByProductId(Long productId);
}

