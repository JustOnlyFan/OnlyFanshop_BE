package com.example.onlyfanshop_be.repository;


import com.example.onlyfanshop_be.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    // Legacy method for backward compatibility
    @Deprecated
    default Product findByProductID(int productID) {
        return findById(productID).orElse(null);
    }

    List<Product> findByBrandId(Integer brandId);

    List<Product> findByCategoryId(Integer categoryId);

    @Override
    @EntityGraph(attributePaths = {"brand", "category", "warranty"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);
    
    @EntityGraph(attributePaths = {"brand", "category", "warranty"})
    java.util.Optional<Product> findById(Integer id);
    
    @Query("SELECT MAX(p.basePrice) FROM Product p WHERE p.status = 'active'")
    java.math.BigDecimal findMaxPrice();

    @Query("SELECT MIN(p.basePrice) FROM Product p WHERE p.status = 'active'")
    java.math.BigDecimal findMinPrice();
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.brandId = :brandId")
    Long countByBrandId(Integer brandId);
    
    // Legacy methods for backward compatibility
    @Deprecated
    default List<Product> findByBrand_BrandID(Integer brandBrandID) {
        return findByBrandId(brandBrandID);
    }
    
    @Deprecated
    default List<Product> findByCategory_CategoryID(Integer categoryCategoryID) {
        return findByCategoryId(categoryCategoryID);
    }
}

