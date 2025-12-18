package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.ProductTag;
import com.example.onlyfanshop_be.entity.Tag;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.ProductTagRepository;
import com.example.onlyfanshop_be.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing product-tag relationships.
 * Handles the many-to-many relationship between products and tags,
 * allowing products to be assigned multiple tags with optional validity periods.
 * 
 * Requirements: 3.2
 */
@Service
public class ProductTagService {

    @Autowired
    private ProductTagRepository productTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Assign multiple tags to a product.
     * This method adds new tag assignments without removing existing ones.
     * 
     * @param productId the product ID
     * @param tagIds list of tag IDs to assign
     * @throws AppException if product or any tag doesn't exist
     */
    @Transactional
    public void assignTagsToProduct(Long productId, List<Integer> tagIds) {
        // Validate product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        for (Integer tagId : tagIds) {
            // Validate tag exists
            if (!tagRepository.existsById(tagId)) {
                throw new RuntimeException("Không tìm thấy tag có ID: " + tagId);
            }

            // Check if relationship already exists
            if (!productTagRepository.existsByProductIdAndTagId(productId, tagId)) {
                ProductTag productTag = ProductTag.builder()
                        .productId(productId)
                        .tagId(tagId)
                        .build();
                productTagRepository.save(productTag);
            }
        }
    }

    /**
     * Assign a tag to a product with validity period.
     * 
     * @param productId the product ID
     * @param tagId the tag ID
     * @param validFrom start date/time when the tag becomes active (null for immediate)
     * @param validUntil end date/time when the tag expires (null for never)
     * @throws AppException if product or tag doesn't exist
     */
    @Transactional
    public void assignTagToProductWithValidity(Long productId, Integer tagId, 
                                                LocalDateTime validFrom, LocalDateTime validUntil) {
        // Validate product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        // Validate tag exists
        if (!tagRepository.existsById(tagId)) {
            throw new RuntimeException("Không tìm thấy tag có ID: " + tagId);
        }

        // Check if relationship already exists
        ProductTag existingProductTag = productTagRepository
                .findByProductIdAndTagId(productId, tagId)
                .orElse(null);

        if (existingProductTag != null) {
            // Update validity period
            existingProductTag.setValidFrom(validFrom);
            existingProductTag.setValidUntil(validUntil);
            productTagRepository.save(existingProductTag);
        } else {
            // Create new relationship
            ProductTag productTag = ProductTag.builder()
                    .productId(productId)
                    .tagId(tagId)
                    .validFrom(validFrom)
                    .validUntil(validUntil)
                    .build();
            productTagRepository.save(productTag);
        }
    }

    /**
     * Assign tags to a product, replacing all existing assignments.
     * 
     * @param productId the product ID
     * @param tagIds list of tag IDs to assign
     * @throws AppException if product or any tag doesn't exist
     */
    @Transactional
    public void replaceProductTags(Long productId, List<Integer> tagIds) {
        // Validate product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        // Remove all existing tag assignments
        productTagRepository.deleteByProductId(productId);

        // Assign new tags
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Integer tagId : tagIds) {
                // Validate tag exists
                if (!tagRepository.existsById(tagId)) {
                    throw new RuntimeException("Không tìm thấy tag có ID: " + tagId);
                }

                ProductTag productTag = ProductTag.builder()
                        .productId(productId)
                        .tagId(tagId)
                        .build();
                productTagRepository.save(productTag);
            }
        }
    }

    /**
     * Remove a specific tag from a product.
     * 
     * @param productId the product ID
     * @param tagId the tag ID to remove
     */
    @Transactional
    public void removeTagFromProduct(Long productId, Integer tagId) {
        // Validate product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        productTagRepository.deleteByProductIdAndTagId(productId, tagId);
    }

    /**
     * Get all tags assigned to a product (regardless of validity).
     * 
     * @param productId the product ID
     * @return list of tags assigned to the product
     */
    public List<Tag> getProductTags(Long productId) {
        // Validate product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        List<ProductTag> productTags = productTagRepository.findByProductIdWithTag(productId);
        return productTags.stream()
                .map(ProductTag::getTag)
                .collect(Collectors.toList());
    }

    /**
     * Get all currently active tags for a product.
     * A tag is active if:
     * - validFrom is null OR validFrom <= current time
     * - validUntil is null OR validUntil >= current time
     * 
     * @param productId the product ID
     * @return list of active tags for the product
     */
    public List<Tag> getActiveProductTags(Long productId) {
        // Validate product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        List<ProductTag> productTags = productTagRepository.findActiveTagsByProductIdWithTag(productId);
        return productTags.stream()
                .map(ProductTag::getTag)
                .collect(Collectors.toList());
    }

    /**
     * Get all tags for a product that are active at a specific time.
     * 
     * @param productId the product ID
     * @param dateTime the date/time to check
     * @return list of tags active at the specified time
     */
    public List<Tag> getProductTagsActiveAt(Long productId, LocalDateTime dateTime) {
        // Validate product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        List<ProductTag> productTags = productTagRepository
                .findActiveTagsByProductIdAtTime(productId, dateTime);
        
        // Need to load tags manually since the query doesn't fetch them
        return productTags.stream()
                .map(pt -> tagRepository.findById(pt.getTagId()).orElse(null))
                .filter(tag -> tag != null)
                .collect(Collectors.toList());
    }

    /**
     * Get all product-tag relationships for a product (with full details).
     * 
     * @param productId the product ID
     * @return list of product-tag relationships
     */
    public List<ProductTag> getProductTagRelationships(Long productId) {
        // Validate product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        return productTagRepository.findByProductIdWithTag(productId);
    }

    /**
     * Check if a product has a specific tag assigned.
     * 
     * @param productId the product ID
     * @param tagId the tag ID
     * @return true if the product has the tag assigned
     */
    public boolean hasTag(Long productId, Integer tagId) {
        return productTagRepository.existsByProductIdAndTagId(productId, tagId);
    }

    /**
     * Check if a product has a specific tag code assigned and currently active.
     * 
     * @param productId the product ID
     * @param tagCode the tag code (e.g., NEW, BESTSELLER)
     * @return true if the product has the tag assigned and active
     */
    public boolean hasActiveTagCode(Long productId, String tagCode) {
        List<Long> productIds = productTagRepository.findProductIdsByActiveTagCode(tagCode);
        return productIds.contains(productId);
    }

    /**
     * Get all product IDs that have a specific tag assigned.
     * 
     * @param tagId the tag ID
     * @return list of product IDs
     */
    public List<Long> getProductIdsByTag(Integer tagId) {
        List<ProductTag> productTags = productTagRepository.findByTagId(tagId);
        return productTags.stream()
                .map(ProductTag::getProductId)
                .collect(Collectors.toList());
    }

    /**
     * Get all product IDs that have a specific tag currently active.
     * 
     * @param tagId the tag ID
     * @return list of product IDs with active tag
     */
    public List<Long> getProductIdsByActiveTag(Integer tagId) {
        List<ProductTag> productTags = productTagRepository.findActiveProductsByTagId(tagId);
        return productTags.stream()
                .map(ProductTag::getProductId)
                .collect(Collectors.toList());
    }

    /**
     * Get all product IDs that have a specific tag code currently active.
     * 
     * @param tagCode the tag code (e.g., NEW, BESTSELLER)
     * @return list of product IDs with active tag
     */
    public List<Long> getProductIdsByActiveTagCode(String tagCode) {
        return productTagRepository.findProductIdsByActiveTagCode(tagCode);
    }

    /**
     * Get the count of tags assigned to a product.
     * 
     * @param productId the product ID
     * @return the number of tags assigned
     */
    public long getTagCount(Long productId) {
        // Validate product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        return productTagRepository.countByProductId(productId);
    }

    /**
     * Get the count of products with a specific tag.
     * 
     * @param tagId the tag ID
     * @return the number of products with the tag
     */
    public long getProductCountByTag(Integer tagId) {
        return productTagRepository.countByTagId(tagId);
    }

    /**
     * Update the validity period for a product-tag relationship.
     * 
     * @param productId the product ID
     * @param tagId the tag ID
     * @param validFrom new start date/time (null for immediate)
     * @param validUntil new end date/time (null for never)
     * @throws RuntimeException if the relationship doesn't exist
     */
    @Transactional
    public void updateTagValidity(Long productId, Integer tagId, 
                                   LocalDateTime validFrom, LocalDateTime validUntil) {
        ProductTag productTag = productTagRepository
                .findByProductIdAndTagId(productId, tagId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy quan hệ product-tag cho productId: " + productId + ", tagId: " + tagId));

        productTag.setValidFrom(validFrom);
        productTag.setValidUntil(validUntil);
        productTagRepository.save(productTag);
    }

    /**
     * Remove all tags from a product.
     * 
     * @param productId the product ID
     */
    @Transactional
    public void removeAllTagsFromProduct(Long productId) {
        // Validate product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        productTagRepository.deleteByProductId(productId);
    }
}
