package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.service.FirebaseStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin("*")
public class FileUploadController {

    @Autowired
    private FirebaseStorageService firebaseStorageService;

    @PostMapping(value = "/image", consumes = "multipart/form-data")
    @Operation(summary = "Upload image to Firebase")
    public ApiResponse<String> uploadImage(
            @Parameter(
                    description = "File image upload",
                    required = true,
                    content = @Content(mediaType = "multipart/form-data",
                            schema = @Schema(type = "string", format = "binary"))
            )
            @RequestParam("file") MultipartFile file
    ) {
        try {
            String imageUrl = firebaseStorageService.uploadFile(file);
            return ApiResponse.<String>builder()
                    .statusCode(200)
                    .message("Upload thành công")
                    .data(imageUrl)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.<String>builder()
                    .statusCode(500)
                    .message("Upload thất bại: " + e.getMessage())
                    .build();
        }
    }
    @DeleteMapping("/image")
    @Operation(summary = "Xóa ảnh trong Firebase Storage theo URL")
    public ApiResponse<String> deleteImage(@RequestParam("url") String imageUrl) {
        try {
            firebaseStorageService.deleteFileByUrl(imageUrl);
            return ApiResponse.<String>builder()
                    .statusCode(200)
                    .message("Xóa ảnh thành công")
                    .data(imageUrl)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.<String>builder()
                    .statusCode(500)
                    .message("Xóa ảnh thất bại: " + e.getMessage())
                    .build();
        }
    }
    @PostMapping(value = "/change", consumes = "multipart/form-data")
    @Operation(summary = "Thay đổi ảnh: xóa ảnh cũ và upload ảnh mới")
    public ApiResponse<String> changeImage(
            @Parameter(
                    description = "File ảnh mới cần upload",
                    required = true,
                    content = @Content(mediaType = "multipart/form-data",
                            schema = @Schema(type = "string", format = "binary"))
            )
            @RequestParam("file") MultipartFile newFile,
            @RequestParam("oldUrl") String oldUrl
    ) {
        try {
            // 🧩 1. Xóa ảnh cũ (nếu có)
            if (oldUrl != null && !oldUrl.isEmpty()) {
                try {
                    firebaseStorageService.deleteFileByUrl(oldUrl);
                } catch (Exception e) {
                    System.err.println("⚠️ Không thể xóa ảnh cũ: " + e.getMessage());
                }
            }

            // 🧩 2. Upload ảnh mới
            String newImageUrl = firebaseStorageService.uploadFile(newFile);

            // ✅ 3. Trả về kết quả
            return ApiResponse.<String>builder()
                    .statusCode(200)
                    .message("Đổi ảnh thành công")
                    .data(newImageUrl)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.<String>builder()
                    .statusCode(500)
                    .message("Đổi ảnh thất bại: " + e.getMessage())
                    .build();
        }
    }

}
