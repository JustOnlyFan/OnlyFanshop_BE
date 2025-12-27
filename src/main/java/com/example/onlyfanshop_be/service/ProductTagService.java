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

@Service
public class ProductTagService {

    @Autowired
    private ProductTagRepository productTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ProductRepository productRepository;

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

    @Transactional
    public void assignTagToProductWithValidity(Long productId, Integer tagId, 
                                                LocalDateTime validFrom, LocalDateTime validUntil) {
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        if (!tagRepository.existsById(tagId)) {
            throw new RuntimeException("Không tìm thấy tag có ID: " + tagId);
        }

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

    @Transactional
    public void replaceProductTags(Long productId, List<Integer> tagIds) {
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        productTagRepository.deleteByProductId(productId);

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

    @Transactional
    public void removeTagFromProduct(Long productId, Integer tagId) {
        // Validate product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        productTagRepository.deleteByProductIdAndTagId(productId, tagId);
    }

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


}
