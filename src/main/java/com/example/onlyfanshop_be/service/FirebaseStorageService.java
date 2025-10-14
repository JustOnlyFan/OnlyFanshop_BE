package com.example.onlyfanshop_be.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class FirebaseStorageService {

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Bucket bucket = StorageClient.getInstance().bucket();

        // Upload file
        Blob blob = bucket.create("products/" + fileName, file.getBytes(), file.getContentType());

        // Trả về URL công khai
        return String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                bucket.getName(),
                blob.getName().replace("/", "%2F")
        );
    }
    public void deleteFileByUrl(String imageUrl) {
        try {
            // Ví dụ URL: https://firebasestorage.googleapis.com/v0/b/koi-farm-shop.appspot.com/o/products%2Fimage.jpg?alt=media&token=1234
            String decodedUrl = java.net.URLDecoder.decode(imageUrl, java.nio.charset.StandardCharsets.UTF_8);

            // Tách phần path giữa "/o/" và "?alt="
            String filePath = decodedUrl.split("/o/")[1].split("\\?alt=")[0];

            Bucket bucket = StorageClient.getInstance().bucket();
            Blob blob = bucket.get(filePath);

            if (blob != null && blob.exists()) {
                blob.delete();
            } else {
                throw new RuntimeException("Không tìm thấy file trong bucket: " + filePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa ảnh: " + e.getMessage(), e);
        }
    }
}
