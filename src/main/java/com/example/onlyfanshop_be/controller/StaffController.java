package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.StaffDTO;
import com.example.onlyfanshop_be.dto.request.CreateStaffRequest;
import com.example.onlyfanshop_be.dto.request.UpdateStaffRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import com.example.onlyfanshop_be.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Staff Controller", description = "APIs for staff management")
public class StaffController {

    private final StaffService staffService;
    private final JwtTokenProvider jwtTokenProvider;

    // Admin endpoints
    @GetMapping("/admin/staff")
    @Operation(summary = "Get all staff", description = "Get paginated list of staff (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<StaffDTO>>> getAllStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer storeLocationId,
            HttpServletRequest httpRequest) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<StaffDTO> staffPage = staffService.getAllStaff(pageable, storeLocationId);
            
            return ResponseEntity.ok(ApiResponse.<Page<StaffDTO>>builder()
                    .statusCode(200)
                    .message("Staff retrieved successfully")
                    .data(staffPage)
                    .build());
        } catch (Exception e) {
            log.error("Error getting staff: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<Page<StaffDTO>>builder()
                    .statusCode(400)
                    .message("Failed to get staff: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/admin/staff/{staffId}")
    @Operation(summary = "Get staff by ID", description = "Get staff details by ID (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StaffDTO>> getStaffById(@PathVariable Long staffId) {
        try {
            StaffDTO staff = staffService.getStaffById(staffId);
            return ResponseEntity.ok(ApiResponse.<StaffDTO>builder()
                    .statusCode(200)
                    .message("Staff retrieved successfully")
                    .data(staff)
                    .build());
        } catch (Exception e) {
            log.error("Error getting staff: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<StaffDTO>builder()
                    .statusCode(400)
                    .message("Failed to get staff: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/admin/staff")
    @Operation(summary = "Create staff", description = "Create a new staff account (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StaffDTO>> createStaff(
            @Valid @RequestBody CreateStaffRequest request) {
        try {
            StaffDTO staff = staffService.createStaff(request);
            return ResponseEntity.ok(ApiResponse.<StaffDTO>builder()
                    .statusCode(201)
                    .message("Staff created successfully")
                    .data(staff)
                    .build());
        } catch (Exception e) {
            log.error("Error creating staff: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<StaffDTO>builder()
                    .statusCode(400)
                    .message("Failed to create staff: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/admin/staff/{staffId}")
    @Operation(summary = "Update staff", description = "Update staff information including store assignment (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StaffDTO>> updateStaff(
            @PathVariable Long staffId,
            @Valid @RequestBody UpdateStaffRequest request) {
        try {
            StaffDTO staff = staffService.updateStaff(staffId, request);
            return ResponseEntity.ok(ApiResponse.<StaffDTO>builder()
                    .statusCode(200)
                    .message("Staff updated successfully")
                    .data(staff)
                    .build());
        } catch (Exception e) {
            log.error("Error updating staff: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<StaffDTO>builder()
                    .statusCode(400)
                    .message("Failed to update staff: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/admin/staff/{staffId}")
    @Operation(summary = "Delete staff", description = "Delete staff account (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable Long staffId) {
        try {
            staffService.deleteStaff(staffId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .statusCode(200)
                    .message("Staff deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error deleting staff: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .statusCode(400)
                    .message("Failed to delete staff: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/admin/staff/store/{storeLocationId}")
    @Operation(summary = "Get staff by store location", description = "Get staff assigned to a specific store (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<StaffDTO>>> getStaffByStoreLocation(
            @PathVariable Integer storeLocationId) {
        try {
            List<StaffDTO> staffList = staffService.getStaffByStoreLocation(storeLocationId);
            return ResponseEntity.ok(ApiResponse.<List<StaffDTO>>builder()
                    .statusCode(200)
                    .message("Staff retrieved successfully")
                    .data(staffList)
                    .build());
        } catch (Exception e) {
            log.error("Error getting staff by store: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<List<StaffDTO>>builder()
                    .statusCode(400)
                    .message("Failed to get staff: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/admin/staff/{staffId}/reset-password")
    @Operation(summary = "Reset staff password", description = "Reset staff password to default 'Staff@123' (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetStaffPassword(@PathVariable Long staffId) {
        try {
            staffService.resetStaffPassword(staffId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .statusCode(200)
                    .message("Staff password reset to default successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error resetting staff password: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .statusCode(400)
                    .message("Failed to reset password: " + e.getMessage())
                    .build());
        }
    }

    // Staff endpoints
    @GetMapping("/staff/profile")
    @Operation(summary = "Get staff profile", description = "Get current staff profile")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<StaffDTO>> getMyProfile(HttpServletRequest httpRequest) {
        try {
            String token = jwtTokenProvider.extractToken(httpRequest);
            Long staffId = Long.parseLong(jwtTokenProvider.getUserIdFromJWT(token).toString());
            
            StaffDTO staff = staffService.getMyProfile(staffId);
            return ResponseEntity.ok(ApiResponse.<StaffDTO>builder()
                    .statusCode(200)
                    .message("Profile retrieved successfully")
                    .data(staff)
                    .build());
        } catch (Exception e) {
            log.error("Error getting staff profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<StaffDTO>builder()
                    .statusCode(400)
                    .message("Failed to get profile: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/staff/profile")
    @Operation(summary = "Update staff profile", description = "Update current staff profile (cannot change store assignment)")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<StaffDTO>> updateMyProfile(
            @Valid @RequestBody UpdateStaffRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = jwtTokenProvider.extractToken(httpRequest);
            Long staffId = Long.parseLong(jwtTokenProvider.getUserIdFromJWT(token).toString());
            
            StaffDTO staff = staffService.updateMyProfile(staffId, request);
            return ResponseEntity.ok(ApiResponse.<StaffDTO>builder()
                    .statusCode(200)
                    .message("Profile updated successfully")
                    .data(staff)
                    .build());
        } catch (Exception e) {
            log.error("Error updating staff profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<StaffDTO>builder()
                    .statusCode(400)
                    .message("Failed to update profile: " + e.getMessage())
                    .build());
        }
    }
}




