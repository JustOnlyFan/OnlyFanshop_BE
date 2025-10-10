package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.service.IStoreLocation;
import com.example.onlyfanshop_be.service.StoreLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/store-locations")
@RequiredArgsConstructor
public class StoreLocationController {

    private final IStoreLocation iStoreLocation;

    // 🟢 Lấy tất cả địa điểm
    @GetMapping
    public ApiResponse<List<StoreLocation>> getAll() {
        return ApiResponse.<List<StoreLocation>>builder()
                .statusCode(200)
                .message("Danh sách địa điểm cửa hàng")
                .data(iStoreLocation.getAllLocations())
                .build();
    }

    // 🟢 Lấy 1 địa điểm theo ID
    @GetMapping("/{id}")
    public ApiResponse<StoreLocation> getById(@PathVariable Integer id) {
        return ApiResponse.<StoreLocation>builder()
                .statusCode(200)
                .message("Chi tiết địa điểm")
                .data(iStoreLocation.getLocationById(id))
                .build();
    }

    // 🟢 Thêm mới
    @PostMapping
    public ApiResponse<StoreLocation> create(@RequestBody StoreLocation location) {
        return ApiResponse.<StoreLocation>builder()
                .statusCode(201)
                .message("Tạo địa điểm thành công")
                .data(iStoreLocation.createLocation(location))
                .build();
    }

    // 🟢 Cập nhật
    @PutMapping("/{id}")
    public ApiResponse<StoreLocation> update(@PathVariable Integer id, @RequestBody StoreLocation location) {
        return ApiResponse.<StoreLocation>builder()
                .statusCode(200)
                .message("Cập nhật địa điểm thành công")
                .data(iStoreLocation.updateLocation(id, location))
                .build();
    }

    // 🟢 Xóa
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        iStoreLocation.deleteLocation(id);
        return ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Xóa địa điểm thành công")
                .build();
    }
}

