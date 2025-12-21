package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Tag;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.TagRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Tag uniqueness constraint.
 * 
 * **Feature: expanded-category-system, Property 9: Tag Uniqueness**
 * **Validates: Requirements 3.1**
 */
class TagUniquenessPropertyTest {

    /**
     * **Feature: expanded-category-system, Property 9: Tag Uniqueness**
     * **Validates: Requirements 3.1**
     * 
     * Property: For any tag code, there SHALL be at most one tag with that code in the system.
     * When attempting to create a tag with an existing code, the system SHALL reject it with DUPLICATE_TAG error.
     */
    @Property(tries = 100)
    void duplicateTagCodeShouldBeRejected(
            @ForAll("validTagCodes") String tagCode,
            @ForAll("validDisplayNames") String displayName) {
        
        // Create a mock repository
        TagRepository mockRepository = mock(TagRepository.class);
        
        // Setup: tag with this code already exists
        String normalizedCode = tagCode.toUpperCase().trim();
        when(mockRepository.existsByCode(normalizedCode)).thenReturn(true);
        
        // Create a tag validator that mimics TagService behavior
        TagUniquenessValidator validator = new TagUniquenessValidator(mockRepository);
        
        // Attempt to create tag with duplicate code should throw exception
        Tag newTag = Tag.builder()
                .code(tagCode)
                .displayName(displayName)
                .build();
        
        assertThatThrownBy(() -> validator.validateAndCreateTag(newTag))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_TAG);
                });
        
        // Verify save was never called
        verify(mockRepository, never()).save(any(Tag.class));
    }

    /**
     * **Feature: expanded-category-system, Property 9: Tag Uniqueness**
     * **Validates: Requirements 3.1**
     * 
     * Property: For any unique tag code, the system SHALL allow creating a tag with that code.
     */
    @Property(tries = 100)
    void uniqueTagCodeShouldBeAccepted(
            @ForAll("validTagCodes") String tagCode,
            @ForAll("validDisplayNames") String displayName) {
        
        // Create a mock repository
        TagRepository mockRepository = mock(TagRepository.class);
        
        // Setup: no tag with this code exists
        String normalizedCode = tagCode.toUpperCase().trim();
        when(mockRepository.existsByCode(normalizedCode)).thenReturn(false);
        
        // Setup: save returns the tag with an ID
        when(mockRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(1);
            return tag;
        });
        
        // Create a tag validator
        TagUniquenessValidator validator = new TagUniquenessValidator(mockRepository);
        
        // Create tag with unique code should succeed
        Tag newTag = Tag.builder()
                .code(tagCode)
                .displayName(displayName)
                .build();
        
        Tag createdTag = validator.validateAndCreateTag(newTag);
        
        // Verify tag was created with normalized code
        assertThat(createdTag).isNotNull();
        assertThat(createdTag.getCode()).isEqualTo(normalizedCode);
        
        // Verify save was called
        verify(mockRepository).save(any(Tag.class));
    }

    /**
     * **Feature: expanded-category-system, Property 9: Tag Uniqueness**
     * **Validates: Requirements 3.1**
     * 
     * Property: Tag codes should be case-insensitive for uniqueness check.
     * Creating a tag with code "NEW" should fail if "new" already exists.
     */
    @Property(tries = 100)
    void tagCodeUniquenessIsCaseInsensitive(
            @ForAll("validTagCodes") String tagCode,
            @ForAll("validDisplayNames") String displayName) {
        
        // Create a mock repository
        TagRepository mockRepository = mock(TagRepository.class);
        
        // Setup: tag with uppercase version of code exists
        String normalizedCode = tagCode.toUpperCase().trim();
        when(mockRepository.existsByCode(normalizedCode)).thenReturn(true);
        
        // Create a tag validator
        TagUniquenessValidator validator = new TagUniquenessValidator(mockRepository);
        
        // Try to create with lowercase version - should still fail
        Tag newTag = Tag.builder()
                .code(tagCode.toLowerCase())
                .displayName(displayName)
                .build();
        
        assertThatThrownBy(() -> validator.validateAndCreateTag(newTag))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_TAG);
                });
    }

    /**
     * **Feature: expanded-category-system, Property 9: Tag Uniqueness**
     * **Validates: Requirements 3.1**
     * 
     * Property: When updating a tag, changing to an existing code should be rejected.
     */
    @Property(tries = 100)
    void updateToExistingCodeShouldBeRejected(
            @ForAll("validTagCodes") String existingCode,
            @ForAll("validTagCodes") String newCode,
            @ForAll("validDisplayNames") String displayName) {
        
        // Skip if codes are the same (not a real update scenario)
        String normalizedExisting = existingCode.toUpperCase().trim();
        String normalizedNew = newCode.toUpperCase().trim();
        if (normalizedExisting.equals(normalizedNew)) {
            return;
        }
        
        // Create a mock repository
        TagRepository mockRepository = mock(TagRepository.class);
        
        // Setup: existing tag to update
        Tag existingTag = Tag.builder()
                .id(1)
                .code(normalizedExisting)
                .displayName(displayName)
                .build();
        
        when(mockRepository.findById(1)).thenReturn(Optional.of(existingTag));
        
        // Setup: new code already exists (belongs to another tag)
        when(mockRepository.existsByCode(normalizedNew)).thenReturn(true);
        
        // Create a tag validator
        TagUniquenessValidator validator = new TagUniquenessValidator(mockRepository);
        
        // Attempt to update to existing code should throw exception
        Tag updateData = Tag.builder()
                .code(newCode)
                .displayName(displayName)
                .build();
        
        assertThatThrownBy(() -> validator.validateAndUpdateTag(1, updateData))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_TAG);
                });
        
        // Verify save was never called
        verify(mockRepository, never()).save(any(Tag.class));
    }

    /**
     * **Feature: expanded-category-system, Property 9: Tag Uniqueness**
     * **Validates: Requirements 3.1**
     * 
     * Property: When updating a tag, keeping the same code should be allowed.
     */
    @Property(tries = 100)
    void updateWithSameCodeShouldBeAllowed(
            @ForAll("validTagCodes") String tagCode,
            @ForAll("validDisplayNames") String displayName,
            @ForAll("validDisplayNames") String newDisplayName) {
        
        // Create a mock repository
        TagRepository mockRepository = mock(TagRepository.class);
        
        String normalizedCode = tagCode.toUpperCase().trim();
        
        // Setup: existing tag to update
        Tag existingTag = Tag.builder()
                .id(1)
                .code(normalizedCode)
                .displayName(displayName)
                .build();
        
        when(mockRepository.findById(1)).thenReturn(Optional.of(existingTag));
        when(mockRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Create a tag validator
        TagUniquenessValidator validator = new TagUniquenessValidator(mockRepository);
        
        // Update with same code should succeed
        Tag updateData = Tag.builder()
                .code(tagCode) // Same code
                .displayName(newDisplayName)
                .build();
        
        Tag updatedTag = validator.validateAndUpdateTag(1, updateData);
        
        // Verify update succeeded
        assertThat(updatedTag).isNotNull();
        assertThat(updatedTag.getCode()).isEqualTo(normalizedCode);
        
        // Verify save was called
        verify(mockRepository).save(any(Tag.class));
    }

    @Provide
    Arbitrary<String> validTagCodes() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(2)
                .ofMaxLength(20)
                .filter(s -> !s.trim().isEmpty());
    }

    @Provide
    Arbitrary<String> validDisplayNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(2)
                .ofMaxLength(50)
                .filter(s -> !s.trim().isEmpty());
    }

    /**
     * Helper class that replicates the tag uniqueness validation logic from TagService.
     * This allows testing the uniqueness constraint without full Spring context.
     */
    static class TagUniquenessValidator {
        private final TagRepository repository;
        
        TagUniquenessValidator(TagRepository repository) {
            this.repository = repository;
        }
        
        Tag validateAndCreateTag(Tag tag) {
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
            if (repository.existsByCode(normalizedCode)) {
                throw new AppException(ErrorCode.DUPLICATE_TAG);
            }

            // Create new tag
            Tag newTag = Tag.builder()
                    .code(normalizedCode)
                    .displayName(tag.getDisplayName().trim())
                    .badgeColor(tag.getBadgeColor())
                    .displayOrder(tag.getDisplayOrder() != null ? tag.getDisplayOrder() : 0)
                    .build();

            return repository.save(newTag);
        }
        
        Tag validateAndUpdateTag(Integer id, Tag updatedTag) {
            Tag existingTag = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tag có ID: " + id));

            // Update code if provided and different
            if (updatedTag.getCode() != null && !updatedTag.getCode().trim().isEmpty()) {
                String newCode = updatedTag.getCode().toUpperCase().trim();
                
                // Check if code is changing and new code already exists
                if (!existingTag.getCode().equals(newCode) && repository.existsByCode(newCode)) {
                    throw new AppException(ErrorCode.DUPLICATE_TAG);
                }
                
                existingTag.setCode(newCode);
            }

            // Update display name if provided
            if (updatedTag.getDisplayName() != null && !updatedTag.getDisplayName().trim().isEmpty()) {
                existingTag.setDisplayName(updatedTag.getDisplayName().trim());
            }

            return repository.save(existingTag);
        }
    }
}
