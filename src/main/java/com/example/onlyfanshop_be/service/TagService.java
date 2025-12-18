package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Tag;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing Tag entities.
 * Handles CRUD operations for product tags used for marketing purposes.
 * Tags can be used to highlight products as new, bestseller, on-sale, premium, imported, or authentic.
 * 
 * Requirements: 3.1
 */
@Service
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    /**
     * Get all tags ordered by display order.
     * 
     * @return list of all tags
     */
    public List<Tag> getAllTags() {
        return tagRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * Get a tag by its ID.
     * 
     * @param id the tag ID
     * @return the tag
     * @throws AppException if tag not found
     */
    public Tag getTagById(Integer id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tag có ID: " + id));
    }

    /**
     * Get a tag by its unique code.
     * 
     * @param code the tag code (e.g., NEW, BESTSELLER, SALE)
     * @return the tag if found
     */
    public Optional<Tag> getTagByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        return tagRepository.findByCode(code.toUpperCase().trim());
    }

    /**
     * Check if a tag with the given code exists.
     * 
     * @param code the tag code to check
     * @return true if a tag with the code exists
     */
    public boolean existsByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return tagRepository.existsByCode(code.toUpperCase().trim());
    }

    /**
     * Create a new tag.
     * Validates that the tag code is unique before creation.
     * 
     * @param tag the tag to create
     * @return the created tag
     * @throws AppException if tag code already exists
     */
    @Transactional
    public Tag createTag(Tag tag) {
        // Validate tag code is provided
        if (tag.getCode() == null || tag.getCode().trim().isEmpty()) {
            throw new RuntimeException("Mã tag không được để trống");
        }

        // Validate display name is provided
        if (tag.getDisplayName() == null || tag.getDisplayName().trim().isEmpty()) {
            throw new RuntimeException("Tên hiển thị tag không được để trống");
        }

        // Normalize code to uppercase
        String normalizedCode = tag.getCode().toUpperCase().trim();

        // Check for uniqueness
        if (tagRepository.existsByCode(normalizedCode)) {
            throw new AppException(ErrorCode.DUPLICATE_TAG);
        }

        // Create new tag
        Tag newTag = Tag.builder()
                .code(normalizedCode)
                .displayName(tag.getDisplayName().trim())
                .badgeColor(tag.getBadgeColor())
                .displayOrder(tag.getDisplayOrder() != null ? tag.getDisplayOrder() : 0)
                .build();

        return tagRepository.save(newTag);
    }

    /**
     * Update an existing tag.
     * Validates that the new code (if changed) is unique.
     * 
     * @param id the tag ID
     * @param updatedTag the updated tag data
     * @return the updated tag
     * @throws AppException if tag not found or new code already exists
     */
    @Transactional
    public Tag updateTag(Integer id, Tag updatedTag) {
        Tag existingTag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tag có ID: " + id));

        // Update code if provided and different
        if (updatedTag.getCode() != null && !updatedTag.getCode().trim().isEmpty()) {
            String newCode = updatedTag.getCode().toUpperCase().trim();
            
            // Check if code is changing and new code already exists
            if (!existingTag.getCode().equals(newCode) && tagRepository.existsByCode(newCode)) {
                throw new AppException(ErrorCode.DUPLICATE_TAG);
            }
            
            existingTag.setCode(newCode);
        }

        // Update display name if provided
        if (updatedTag.getDisplayName() != null && !updatedTag.getDisplayName().trim().isEmpty()) {
            existingTag.setDisplayName(updatedTag.getDisplayName().trim());
        }

        // Update badge color if provided
        if (updatedTag.getBadgeColor() != null) {
            existingTag.setBadgeColor(updatedTag.getBadgeColor());
        }

        // Update display order if provided
        if (updatedTag.getDisplayOrder() != null) {
            existingTag.setDisplayOrder(updatedTag.getDisplayOrder());
        }

        return tagRepository.save(existingTag);
    }

    /**
     * Delete a tag by its ID.
     * 
     * @param id the tag ID to delete
     * @throws RuntimeException if tag not found
     */
    @Transactional
    public void deleteTag(Integer id) {
        if (!tagRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy tag có ID: " + id);
        }
        tagRepository.deleteById(id);
    }

    /**
     * Get tags by a list of codes.
     * 
     * @param codes list of tag codes
     * @return list of tags matching the codes
     */
    public List<Tag> getTagsByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        
        // Normalize codes to uppercase
        List<String> normalizedCodes = codes.stream()
                .filter(code -> code != null && !code.trim().isEmpty())
                .map(code -> code.toUpperCase().trim())
                .toList();
        
        return tagRepository.findByCodeIn(normalizedCodes);
    }

    /**
     * Validate that a tag code is unique.
     * This method can be used for validation before creating or updating a tag.
     * 
     * @param code the tag code to validate
     * @param excludeId optional tag ID to exclude from the check (for updates)
     * @return true if the code is unique (or belongs to the excluded tag)
     */
    public boolean isCodeUnique(String code, Integer excludeId) {
        if (code == null || code.trim().isEmpty()) {
            return true; // Empty code is not valid but not a duplicate
        }

        String normalizedCode = code.toUpperCase().trim();
        Optional<Tag> existingTag = tagRepository.findByCode(normalizedCode);
        
        if (existingTag.isEmpty()) {
            return true; // No tag with this code exists
        }
        
        // If excludeId is provided, check if the existing tag is the one being updated
        return excludeId != null && existingTag.get().getId().equals(excludeId);
    }

    /**
     * Validate tag code uniqueness and throw exception if not unique.
     * 
     * @param code the tag code to validate
     * @throws AppException if the code already exists
     */
    public void validateCodeUniqueness(String code) {
        if (code != null && !code.trim().isEmpty() && existsByCode(code)) {
            throw new AppException(ErrorCode.DUPLICATE_TAG);
        }
    }
}
