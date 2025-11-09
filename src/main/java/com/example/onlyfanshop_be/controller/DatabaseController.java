package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.service.DatabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho các thao tác quản lý database
 * CHỈ DÀNH CHO ADMIN - CẨN THẬN KHI SỬ DỤNG!
 */
@RestController
@RequestMapping("/api/admin/database")
@RequiredArgsConstructor
@Slf4j
public class DatabaseController {

    private final DatabaseService databaseService;

    /**
     * Xóa toàn bộ dữ liệu trong database
     * ⚠️ CẢNH BÁO: Endpoint này sẽ xóa TẤT CẢ dữ liệu!
     */
    @DeleteMapping("/delete-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteAllData() {
        try {
            databaseService.deleteAllData();
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .statusCode(200)
                    .message("Đã xóa toàn bộ dữ liệu trong database thành công")
                    .data("OK")
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi xóa dữ liệu: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<String>builder()
                            .statusCode(500)
                            .message("Lỗi khi xóa dữ liệu: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    /**
     * Xóa dữ liệu từ một bảng cụ thể
     */
    @DeleteMapping("/delete-table/{tableName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteTableData(@PathVariable String tableName) {
        try {
            databaseService.deleteTableData(tableName);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .statusCode(200)
                    .message("Đã xóa dữ liệu từ bảng " + tableName + " thành công")
                    .data("OK")
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi xóa dữ liệu từ bảng {}: {}", tableName, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<String>builder()
                            .statusCode(500)
                            .message("Lỗi khi xóa dữ liệu từ bảng " + tableName + ": " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    /**
     * Lấy danh sách tất cả các bảng trong database
     */
    @GetMapping("/tables")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getAllTables() {
        try {
            List<String> tables = databaseService.getAllTableNames();
            return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                    .statusCode(200)
                    .data(tables)
                    .message("Lấy danh sách bảng thành công")
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách bảng: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<String>>builder()
                            .statusCode(500)
                            .message("Lỗi khi lấy danh sách bảng: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }
}

