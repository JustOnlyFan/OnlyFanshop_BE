package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);
    
    Optional<ProductVariant> findBySku(String sku);
}



