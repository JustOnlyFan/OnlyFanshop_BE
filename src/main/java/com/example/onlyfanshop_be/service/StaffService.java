package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.StaffDTO;
import com.example.onlyfanshop_be.dto.StoreLocationSummaryDTO;
import com.example.onlyfanshop_be.dto.request.CreateStaffRequest;
import com.example.onlyfanshop_be.dto.request.UpdateStaffRequest;
import com.example.onlyfanshop_be.entity.Role;
import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.UserStatus;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.RoleRepository;
import com.example.onlyfanshop_be.repository.StoreLocationRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StoreLocationRepository storeLocationRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public StaffDTO createStaff(CreateStaffRequest request) {
        // Validate store location is required
        if (request.getStoreLocationId() == null) {
            throw new AppException(ErrorCode.INVALID_INPUT); // Store location is required
        }

        // Get store location
        StoreLocation storeLocation = storeLocationRepository.findById(request.getStoreLocationId())
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
        
        // Check if store already has a staff assigned
        List<User> existingStaff = userRepository.findByStoreLocationId(request.getStoreLocationId());
        if (!existingStaff.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT); // Store already has staff
        }

        // Auto-generate username and email from store if not provided
        String username = request.getUsername();
        String email = request.getEmail();
        String phone = request.getPhoneNumber();

        // Normalize store name: remove special chars, convert to lowercase, remove spaces, remove accents
        String normalizedStoreName = storeLocation.getName()
                .toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .replaceAll("[^a-z0-9\\s]", "") // Remove special chars
                .replaceAll("\\s+", ""); // Remove spaces

        // Email: Use store email if available, otherwise use provided email
        if (email == null || email.trim().isEmpty()) {
            if (storeLocation.getEmail() != null && !storeLocation.getEmail().trim().isEmpty()) {
                email = storeLocation.getEmail();
            } else {
                throw new AppException(ErrorCode.INVALID_INPUT); // Store email is required
            }
        }

        // If phone is not provided, use store phone
        if (phone == null || phone.trim().isEmpty()) {
            phone = storeLocation.getPhone();
        }

        // Validate email uniqueness
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_USED);
        }

        // Get staff role - must exist, try different case variations
        Role staffRole = roleRepository.findByName("staff")
                .orElseGet(() -> {
                    // Try uppercase
                    return roleRepository.findByName("STAFF")
                            .orElseGet(() -> {
                                // Try capitalized
                                return roleRepository.findByName("Staff")
                                        .orElseGet(() -> {
                                            log.error("Staff role not found in database with any case variation. Available roles: {}", 
                                                    roleRepository.findAll().stream().map(Role::getName).toList());
                                            // Create staff role if it doesn't exist
                                            Role newStaffRole = Role.builder()
                                                    .name("staff")
                                                    .description("Nhân viên")
                                                    .build();
                                            Role saved = roleRepository.save(newStaffRole);
                                            log.info("Created missing staff role with ID: {}", saved.getId());
                                            return saved;
                                        });
                            });
                });

        log.info("Using staff role with roleId: {}, roleName: {}", staffRole.getId(), staffRole.getName());

        // Create staff user with temporary username (will be updated after getting the ID)
        // Use temporary username to avoid conflicts
        String tempUsername = "staff_temp_" + System.currentTimeMillis();
        while (userRepository.existsByUsername(tempUsername)) {
            tempUsername = "staff_temp_" + System.currentTimeMillis();
        }

        User staff = User.builder()
                .username(tempUsername)
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roleId(staffRole.getId())
                .storeLocationId(request.getStoreLocationId())
                .status(UserStatus.active)
                .createdAt(LocalDateTime.now())
                .build();

        User savedStaff = userRepository.save(staff);
        
        // Now generate the final username with staff ID
        String finalUsername;
        if (username == null || username.trim().isEmpty()) {
            // Format: staff_ + normalizedStoreName + _ + staffId
            finalUsername = "staff_" + normalizedStoreName + "_" + savedStaff.getId();
            
            // If username exists (shouldn't happen, but just in case), add attempt counter
            int attempt = 0;
            while (userRepository.existsByUsername(finalUsername) && attempt < 10) {
                finalUsername = "staff_" + normalizedStoreName + "_" + savedStaff.getId() + "_" + attempt;
                attempt++;
            }
            if (attempt >= 10) {
                // Fallback: use timestamp
                finalUsername = "staff_" + normalizedStoreName + "_" + savedStaff.getId() + "_" + System.currentTimeMillis();
            }
        } else {
            // Normalize provided username: remove spaces
            finalUsername = username.trim().replaceAll("\\s+", "");
        }

        // Validate final username uniqueness
        if (userRepository.existsByUsername(finalUsername) && !finalUsername.equals(tempUsername)) {
            throw new AppException(ErrorCode.USERNAME_USED);
        }

        // Update username with final value
        savedStaff.setUsername(finalUsername);
        User verifiedStaff = userRepository.save(savedStaff);
        
        // Verify the saved staff has correct role
        log.info("Staff created with ID: {}, username: {}, email: {}, roleId: {}", 
                verifiedStaff.getId(), verifiedStaff.getUsername(), verifiedStaff.getEmail(), verifiedStaff.getRoleId());
        
        return convertToDTO(verifiedStaff);
    }

    @Transactional(readOnly = true)
    public Page<StaffDTO> getAllStaff(Pageable pageable, Integer storeLocationId) {
        Role staffRole = roleRepository.findByName("staff")
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

        Page<User> staffPage;
        if (storeLocationId != null) {
            staffPage = userRepository.findByRoleIdAndStoreLocationId(staffRole.getId(), storeLocationId, pageable);
        } else {
            staffPage = userRepository.findByRoleId(staffRole.getId(), pageable);
        }

        return staffPage.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public StaffDTO getStaffById(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

        Role staffRole = roleRepository.findByName("staff")
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

        if (!staff.getRoleId().equals(staffRole.getId())) {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }

        return convertToDTO(staff);
    }

    @Transactional
    public StaffDTO updateStaff(Long staffId, UpdateStaffRequest request) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

        Role staffRole = roleRepository.findByName("staff")
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

        if (!staff.getRoleId().equals(staffRole.getId())) {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }

        // Update fields
        if (request.getUsername() != null) {
            // Normalize username: remove spaces
            String normalizedUsername = request.getUsername().trim().replaceAll("\\s+", "");
            if (!staff.getUsername().equals(normalizedUsername) && userRepository.existsByUsername(normalizedUsername)) {
                throw new AppException(ErrorCode.USERNAME_USED);
            }
            staff.setUsername(normalizedUsername);
        }

        if (request.getEmail() != null) {
            if (!staff.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_USED);
            }
            staff.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null) {
            staff.setPhone(request.getPhoneNumber());
        }

        if (request.getStatus() != null) {
            staff.setStatus(request.getStatus());
        }

        // Handle store location assignment
        if (request.getStoreLocationId() != null) {
            if (!storeLocationRepository.existsById(request.getStoreLocationId())) {
                throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
            }

            // If assigning a new store, check if it already has staff
            if (!request.getStoreLocationId().equals(staff.getStoreLocationId())) {
                List<User> existingStaff = userRepository.findByStoreLocationId(request.getStoreLocationId());
                if (!existingStaff.isEmpty() && !existingStaff.get(0).getId().equals(staffId)) {
                    throw new AppException(ErrorCode.INVALID_INPUT); // Store already has staff
                }
            }
            staff.setStoreLocationId(request.getStoreLocationId());
        }

        User updatedStaff = userRepository.save(staff);
        return convertToDTO(updatedStaff);
    }

    @Transactional
    public void deleteStaff(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

        Role staffRole = roleRepository.findByName("staff")
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

        if (!staff.getRoleId().equals(staffRole.getId())) {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }

        userRepository.delete(staff);
    }

    @Transactional(readOnly = true)
    public StaffDTO getMyProfile(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

        return convertToDTO(staff);
    }

    @Transactional
    public StaffDTO updateMyProfile(Long staffId, UpdateStaffRequest request) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

        // Staff can only update their own profile (not store assignment or status)
        if (request.getUsername() != null) {
            // Normalize username: remove spaces
            String normalizedUsername = request.getUsername().trim().replaceAll("\\s+", "");
            if (!staff.getUsername().equals(normalizedUsername) && userRepository.existsByUsername(normalizedUsername)) {
                throw new AppException(ErrorCode.USERNAME_USED);
            }
            staff.setUsername(normalizedUsername);
        }

        if (request.getEmail() != null) {
            if (!staff.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_USED);
            }
            staff.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null) {
            staff.setPhone(request.getPhoneNumber());
        }

        User updatedStaff = userRepository.save(staff);
        return convertToDTO(updatedStaff);
    }

    @Transactional(readOnly = true)
    public List<StaffDTO> getStaffByStoreLocation(Integer storeLocationId) {
        List<User> staffList = userRepository.findByStoreLocationId(storeLocationId);
        return staffList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void resetStaffPassword(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

        Role staffRole = roleRepository.findByName("staff")
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

        if (!staff.getRoleId().equals(staffRole.getId())) {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }

        // Reset password to default
        String defaultPassword = "Staff@123";
        staff.setPasswordHash(passwordEncoder.encode(defaultPassword));
        userRepository.save(staff);
        
        log.info("Staff password reset to default for staff ID: {}", staffId);
    }

    private StaffDTO convertToDTO(User user) {
        StoreLocation storeLocation = user.getStoreLocation();
        StoreLocationSummaryDTO storeLocationSummary = null;
        if (storeLocation != null) {
            storeLocationSummary = StoreLocationSummaryDTO.builder()
                    .locationID(storeLocation.getLocationID())
                    .name(storeLocation.getName())
                    .address(storeLocation.getAddress())
                    .ward(storeLocation.getWard())
                    .city(storeLocation.getCity())
                    .phone(storeLocation.getPhone())
                    .status(storeLocation.getStatus())
                    .build();
        }

        return StaffDTO.builder()
                .userID(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhone())
                .address(user.getAddress())
                .role("STAFF")
                .storeLocationId(user.getStoreLocationId())
                .storeLocation(storeLocationSummary)
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}

