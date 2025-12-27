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

@Service
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    public List<Tag> getAllTags() {
        return tagRepository.findAllByOrderByDisplayOrderAsc();
    }

    public Tag getTagById(Integer id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tag có ID: " + id));
    }

    public Optional<Tag> getTagByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        return tagRepository.findByCode(code.toUpperCase().trim());
    }

    public boolean existsByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return tagRepository.existsByCode(code.toUpperCase().trim());
    }

    @Transactional
    public Tag createTag(Tag tag) {

        if (tag.getCode() == null || tag.getCode().trim().isEmpty()) {
            throw new RuntimeException("Mã tag không được để trống");
        }

        if (tag.getDisplayName() == null || tag.getDisplayName().trim().isEmpty()) {
            throw new RuntimeException("Tên hiển thị tag không được để trống");
        }

        String normalizedCode = tag.getCode().toUpperCase().trim();

        if (tagRepository.existsByCode(normalizedCode)) {
            throw new AppException(ErrorCode.DUPLICATE_TAG);
        }

        Tag newTag = Tag.builder()
                .code(normalizedCode)
                .displayName(tag.getDisplayName().trim())
                .badgeColor(tag.getBadgeColor())
                .displayOrder(tag.getDisplayOrder() != null ? tag.getDisplayOrder() : 0)
                .build();

        return tagRepository.save(newTag);
    }

    @Transactional
    public Tag updateTag(Integer id, Tag updatedTag) {
        Tag existingTag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tag có ID: " + id));

        if (updatedTag.getCode() != null && !updatedTag.getCode().trim().isEmpty()) {
            String newCode = updatedTag.getCode().toUpperCase().trim();

            if (!existingTag.getCode().equals(newCode) && tagRepository.existsByCode(newCode)) {
                throw new AppException(ErrorCode.DUPLICATE_TAG);
            }
            
            existingTag.setCode(newCode);
        }

        if (updatedTag.getDisplayName() != null && !updatedTag.getDisplayName().trim().isEmpty()) {
            existingTag.setDisplayName(updatedTag.getDisplayName().trim());
        }

        if (updatedTag.getBadgeColor() != null) {
            existingTag.setBadgeColor(updatedTag.getBadgeColor());
        }

        if (updatedTag.getDisplayOrder() != null) {
            existingTag.setDisplayOrder(updatedTag.getDisplayOrder());
        }

        return tagRepository.save(existingTag);
    }

    @Transactional
    public void deleteTag(Integer id) {
        if (!tagRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy tag có ID: " + id);
        }
        tagRepository.deleteById(id);
    }

}
