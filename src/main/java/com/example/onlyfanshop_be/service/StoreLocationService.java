package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.StoreStatus;
import com.example.onlyfanshop_be.enums.UserStatus;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.StoreLocationRepository;
import com.example.onlyfanshop_be.repository.WarehouseRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class StoreLocationService implements IStoreLocation {
    @Autowired
    private StoreLocationRepository storeLocationRepository;
	@Autowired
	private WarehouseRepository warehouseRepository;
	@Autowired
	private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StoreLocation> getAllLocations() {
        try {
            return storeLocationRepository.findAll();
        } catch (Exception e) {
            // Log error and return empty list if database schema is not updated
            System.err.println("Error loading store locations: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public StoreLocation getLocationById(int id) {
        return storeLocationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
    }

    @Override
    public StoreLocation createLocation(StoreLocation storeLocation) {
        // Fallback server-side guard to avoid null not-null violations
        if (storeLocation.getName() == null || storeLocation.getName().isBlank()
                || storeLocation.getLatitude() == null
                || storeLocation.getLongitude() == null
                || storeLocation.getAddress() == null || storeLocation.getAddress().isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        return storeLocationRepository.save(storeLocation);
    }

    @Override
    public StoreLocation updateLocation(int id, StoreLocation newLocation) {
        StoreLocation existing = getLocationById(id);
        existing.setName(newLocation.getName());
        existing.setDescription(newLocation.getDescription());
        existing.setImageUrl(newLocation.getImageUrl());
        existing.setLatitude(newLocation.getLatitude());
        existing.setLongitude(newLocation.getLongitude());
        existing.setAddress(newLocation.getAddress());
        existing.setWard(newLocation.getWard());
        existing.setCity(newLocation.getCity());
        existing.setPhone(newLocation.getPhone());
        existing.setEmail(newLocation.getEmail());
        existing.setOpeningHours(newLocation.getOpeningHours());
		if (newLocation.getStatus() != null) {
			existing.setStatus(newLocation.getStatus());
		}
        if (existing.getName() == null || existing.getName().isBlank()
                || existing.getLatitude() == null
                || existing.getLongitude() == null
                || existing.getAddress() == null || existing.getAddress().isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
		StoreLocation saved = storeLocationRepository.save(existing);
		synchronizeStaffStatus(saved.getLocationID(), saved.getStatus());
		return saved;
    }

    @Override
    public void deleteLocation(int id) {
		StoreLocation existing = storeLocationRepository.findById(id)
				.orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));

		// If there are warehouses linked to this store, avoid hard delete to prevent FK/cascade issues.
		// Soft-disable the store and its warehouses instead.
		var warehouses = warehouseRepository.findByStoreLocationId(id);
		if (!warehouses.isEmpty()) {
			existing.setStatus(StoreStatus.CLOSED);
			storeLocationRepository.save(existing);
			warehouses.forEach(w -> {
				if (Boolean.TRUE.equals(w.getIsActive())) {
					w.setIsActive(false);
					warehouseRepository.save(w);
				}
			});
			synchronizeStaffStatus(existing.getLocationID(), StoreStatus.CLOSED);
			return;
		}

		// No linked warehouses -> still ensure staff accounts are locked before removal
		synchronizeStaffStatus(existing.getLocationID(), StoreStatus.CLOSED);

		// No linked warehouses -> safe to delete
		storeLocationRepository.deleteById(id);
    }

	@Override
	public void synchronizeStaffStatus(int storeId, StoreStatus status) {
		List<User> staffList = userRepository.findByStoreLocationId(storeId);
		if (staffList.isEmpty()) {
			return;
		}

		UserStatus targetStatus = switch (status) {
			case ACTIVE -> UserStatus.active;
			case PAUSED -> UserStatus.inactive;
			case CLOSED -> UserStatus.banned;
		};

		for (User user : staffList) {
			if (user.getStatus() != targetStatus) {
				user.setStatus(targetStatus);
			}
		}
		userRepository.saveAll(staffList);
	}
}
