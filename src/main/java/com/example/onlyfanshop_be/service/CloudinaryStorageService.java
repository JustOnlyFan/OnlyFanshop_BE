package com.example.onlyfanshop_be.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryStorageService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) throws IOException {
        return uploadFileToFolder(file, "products");
    }

    public String uploadFileToFolder(MultipartFile file, String folderName) throws IOException {
        try {
            String fileName = UUID.randomUUID().toString() + "_" + sanitizeFileName(file.getOriginalFilename());
            String publicId = folderName + "/" + fileName;

            System.out.println("📁 Uploading to Cloudinary folder: " + folderName);
            System.out.println("📄 Public ID: " + publicId);

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "auto",
                            "folder", folderName
                    ));

            String imageUrl = (String) uploadResult.get("secure_url");
            System.out.println("✅ File uploaded successfully to Cloudinary. URL: " + imageUrl);
            return imageUrl;

        } catch (IOException e) {
            System.err.println("❌ Error uploading file to Cloudinary folder " + folderName + ": " + e.getMessage());
            throw new IOException("Lỗi upload file: " + e.getMessage(), e);
        }
    }

    public void deleteFileByUrl(String imageUrl) {
        try {
            String publicId = extractPublicIdFromUrl(imageUrl);

            if (publicId != null && !publicId.isEmpty()) {
                Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                System.out.println("🗑️ Deleted file from Cloudinary: " + publicId + " - Result: " + result.get("result"));
            } else {
                throw new RuntimeException("Không thể trích xuất public_id từ URL: " + imageUrl);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa ảnh từ Cloudinary: " + e.getMessage(), e);
        }
    }

    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            if (imageUrl.contains("/upload/")) {
                String[] parts = imageUrl.split("/upload/");
                if (parts.length > 1) {
                    String pathAfterUpload = parts[1];
                    if (pathAfterUpload.matches("^v\\d+/.*")) {
                        pathAfterUpload = pathAfterUpload.substring(pathAfterUpload.indexOf('/') + 1);
                    }
                    // Remove file extension
                    int lastDotIndex = pathAfterUpload.lastIndexOf('.');
                    if (lastDotIndex > 0) {
                        return pathAfterUpload.substring(0, lastDotIndex);
                    }
                    return pathAfterUpload;
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error extracting public_id from URL: " + e.getMessage());
            return null;
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "unknown_file";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
