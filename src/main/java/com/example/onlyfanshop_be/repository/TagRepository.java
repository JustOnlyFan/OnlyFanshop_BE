package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Tag entities.
 * Handles CRUD operations for product tags used for marketing purposes.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    
    /**
     * Find a tag by its unique code.
     * @param code the tag code (e.g., NEW, BESTSELLER, SALE)
     * @return the tag if found
     */
    Optional<Tag> findByCode(String code);
    
    /**
     * Check if a tag with the given code exists.
     * @param code the tag code to check
     * @return true if a tag with the code exists
     */
    boolean existsByCode(String code);
    
    /**
     * Find all tags ordered by display order.
     * @return list of tags ordered by displayOrder
     */
    List<Tag> findAllByOrderByDisplayOrderAsc();
    
    /**
     * Find tags by a list of codes.
     * @param codes list of tag codes
     * @return list of tags matching the codes
     */
    List<Tag> findByCodeIn(List<String> codes);
    
    /**
     * Find a tag by its display name.
     * @param displayName the display name to search for
     * @return the tag if found
     */
    Optional<Tag> findByDisplayName(String displayName);
    
    /**
     * Check if a tag with the given display name exists.
     * @param displayName the display name to check
     * @return true if a tag with the display name exists
     */
    boolean existsByDisplayName(String displayName);
}
