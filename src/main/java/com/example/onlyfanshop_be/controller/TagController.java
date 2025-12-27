package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.TagDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Tag;
import com.example.onlyfanshop_be.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<TagDTO>>> getAllTags() {
        try {
            List<Tag> tags = tagService.getAllTags();
            List<TagDTO> tagDTOs = tags.stream()
                    .map(TagDTO::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.<List<TagDTO>>builder()
                    .statusCode(200)
                    .message("Lấy danh sách tag thành công")
                    .data(tagDTOs)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<TagDTO>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<TagDTO>> getTagById(@PathVariable Integer id) {
        try {
            Tag tag = tagService.getTagById(id);
            TagDTO tagDTO = TagDTO.fromEntity(tag);
            
            return ResponseEntity.ok(ApiResponse.<TagDTO>builder()
                    .statusCode(200)
                    .message("Lấy tag thành công")
                    .data(tagDTO)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<TagDTO>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/public/code/{code}")
    public ResponseEntity<ApiResponse<TagDTO>> getTagByCode(@PathVariable String code) {
        try {
            return tagService.getTagByCode(code)
                    .map(tag -> ResponseEntity.ok(ApiResponse.<TagDTO>builder()
                            .statusCode(200)
                            .message("Lấy tag thành công")
                            .data(TagDTO.fromEntity(tag))
                            .build()))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.<TagDTO>builder()
                                    .statusCode(404)
                                    .message("Không tìm thấy tag với mã: " + code)
                                    .build()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<TagDTO>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<TagDTO>> createTag(@RequestBody TagDTO tagDTO) {
        try {
            Tag tag = tagDTO.toEntity();
            Tag created = tagService.createTag(tag);
            TagDTO result = TagDTO.fromEntity(created);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<TagDTO>builder()
                            .statusCode(201)
                            .message("Tạo tag thành công")
                            .data(result)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<TagDTO>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<TagDTO>> updateTag(
            @PathVariable Integer id, 
            @RequestBody TagDTO tagDTO) {
        try {
            Tag tag = tagDTO.toEntity();
            Tag updated = tagService.updateTag(id, tag);
            TagDTO result = TagDTO.fromEntity(updated);
            
            return ResponseEntity.ok(ApiResponse.<TagDTO>builder()
                    .statusCode(200)
                    .message("Cập nhật tag thành công")
                    .data(result)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<TagDTO>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Integer id) {
        try {
            tagService.deleteTag(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .statusCode(200)
                    .message("Xóa tag thành công")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Void>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/admin/exists/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Boolean>> checkTagCodeExists(@PathVariable String code) {
        try {
            boolean exists = tagService.existsByCode(code);
            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .statusCode(200)
                    .message(exists ? "Mã tag đã tồn tại" : "Mã tag chưa tồn tại")
                    .data(exists)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Boolean>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }
}
