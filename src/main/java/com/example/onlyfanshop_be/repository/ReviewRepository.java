package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Review;
import com.example.onlyfanshop_be.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);
    
    Page<Review> findByProductId(Long productId, Pageable pageable);
    
    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);
    
    Optional<Review> findByProductIdAndUserIdAndStatus(Long productId, Long userId, ReviewStatus status);
    
    long countByProductIdAndStatus(Long productId, ReviewStatus status);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId AND r.status = :status")
    Double findAverageRatingByProductIdAndStatus(@Param("productId") Long productId, @Param("status") ReviewStatus status);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :productId AND r.status = :status AND r.rating = :rating")
    Long countByProductIdAndStatusAndRating(@Param("productId") Long productId, @Param("status") ReviewStatus status, @Param("rating") Integer rating);
}

