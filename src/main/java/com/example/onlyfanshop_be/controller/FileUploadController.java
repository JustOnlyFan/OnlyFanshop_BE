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
}
