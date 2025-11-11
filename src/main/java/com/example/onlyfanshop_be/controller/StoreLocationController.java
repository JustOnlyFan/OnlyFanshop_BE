package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.request.StoreLocationRequest;
import com.example.onlyfanshop_be.dto.request.CreateWarehouseRequest;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.service.IStoreLocation;
import com.example.onlyfanshop_be.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/store-locations")
@RequiredArgsConstructor
public class StoreLocationController {

    private final IStoreLocation iStoreLocation;
    private final WarehouseService warehouseService;

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
    public ApiResponse<StoreLocation> create(@Valid @RequestBody StoreLocationRequest request) {
        // Enforce regional warehouse parent is provided (always create branch warehouse)
        if (request.getParentRegionalWarehouseId() == null) {
            throw new com.example.onlyfanshop_be.exception.AppException(com.example.onlyfanshop_be.exception.ErrorCode.INVALID_INPUT);
        }
        // Map request to entity, support both phone and phoneNumber, and take first image if provided
        String resolvedPhone = request.getPhone() != null ? request.getPhone() : request.getPhoneNumber();
        String resolvedImage = request.getImageUrl();
        if ((resolvedImage == null || resolvedImage.isBlank()) && request.getImages() != null && !request.getImages().isEmpty()) {
            resolvedImage = request.getImages().get(0);
        }

        StoreLocation location = StoreLocation.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(resolvedImage)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .ward(request.getWard())
                .city(request.getCity())
                .phone(resolvedPhone)
                .openingHours(request.getOpeningHours())
                .isActive(true)
                .build();
        StoreLocation saved = iStoreLocation.createLocation(location);

        // Optionally create a BRANCH warehouse linked to this store under a REGIONAL warehouse parent
        if (request.getParentRegionalWarehouseId() != null) {
            CreateWarehouseRequest wh = new CreateWarehouseRequest();
            wh.setName("Kho " + request.getName());
            wh.setCode("BR-" + saved.getLocationID());
            wh.setType(WarehouseType.BRANCH);
            wh.setParentWarehouseId(request.getParentRegionalWarehouseId());
            wh.setStoreLocationId(saved.getLocationID());
            wh.setAddressLine1(request.getAddress());
            wh.setWard(request.getWard());
            wh.setCity(request.getCity());
            wh.setPhone(resolvedPhone);
            // createdBy is not audited here, pass null
            warehouseService.createWarehouse(wh, null);
        }

        return ApiResponse.<StoreLocation>builder()
                .statusCode(201)
                .message("Tạo địa điểm thành công")
                .data(saved)
                .build();
    }

    // 🟢 Cập nhật
    @PutMapping("/{id}")
    public ApiResponse<StoreLocation> update(@PathVariable Integer id, @Valid @RequestBody StoreLocationRequest request) {
        StoreLocation location = StoreLocation.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .phone(request.getPhone())
                .openingHours(request.getOpeningHours())
                .build();
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

