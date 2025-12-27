package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {

    List<ProductTag> findByProductId(Long productId);

    @Query("SELECT pt FROM ProductTag pt JOIN FETCH pt.tag WHERE pt.productId = :productId")
    List<ProductTag> findByProductIdWithTag(@Param("productId") Long productId);

    @Query("SELECT pt FROM ProductTag pt JOIN FETCH pt.tag WHERE pt.productId = :productId " +
           "AND (pt.validFrom IS NULL OR pt.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (pt.validUntil IS NULL OR pt.validUntil >= CURRENT_TIMESTAMP)")
    List<ProductTag> findActiveTagsByProductIdWithTag(@Param("productId") Long productId);

    @Query("SELECT pt FROM ProductTag pt WHERE pt.productId = :productId " +
           "AND (pt.validFrom IS NULL OR pt.validFrom <= :dateTime) " +
           "AND (pt.validUntil IS NULL OR pt.validUntil >= :dateTime)")
    List<ProductTag> findActiveTagsByProductIdAtTime(@Param("productId") Long productId, @Param("dateTime") LocalDateTime dateTime);

    boolean existsByProductIdAndTagId(Long productId, Integer tagId);

    Optional<ProductTag> findByProductIdAndTagId(Long productId, Integer tagId);

    void deleteByProductId(Long productId);

    void deleteByProductIdAndTagId(Long productId, Integer tagId);

    List<ProductTag> findByTagId(Integer tagId);

    @Query("SELECT pt FROM ProductTag pt WHERE pt.tagId = :tagId " +
           "AND (pt.validFrom IS NULL OR pt.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (pt.validUntil IS NULL OR pt.validUntil >= CURRENT_TIMESTAMP)")
    List<ProductTag> findActiveProductsByTagId(@Param("tagId") Integer tagId);

    long countByProductId(Long productId);

    long countByTagId(Integer tagId);

    @Query("SELECT pt.productId FROM ProductTag pt JOIN pt.tag t WHERE t.code = :tagCode " +
           "AND (pt.validFrom IS NULL OR pt.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (pt.validUntil IS NULL OR pt.validUntil >= CURRENT_TIMESTAMP)")
    List<Long> findProductIdsByActiveTagCode(@Param("tagCode") String tagCode);
}
