package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.StoreStatus;
import com.example.onlyfanshop_be.enums.UserStatus;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.StoreLocationRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
@Service
public class StoreLocationService implements IStoreLocation {
    @Autowired
    private StoreLocationRepository storeLocationRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private StoreInventoryService storeInventoryService;

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

		// Ensure staff accounts are locked before removal
		synchronizeStaffStatus(existing.getLocationID(), StoreStatus.CLOSED);

		// Safe to delete
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

	@Override
	@Transactional(readOnly = true)
	public List<StoreLocation> getStoresWithProduct(Long productId, String city, String district) {
		// Sử dụng StoreInventoryService để lấy danh sách stores có bán sản phẩm (isAvailable = true)
		List<StoreLocation> stores = storeInventoryService.getStoresWithProduct(productId);
		
		// Filter by city if provided, and only active stores
		List<StoreLocation> filteredStores = stores.stream()
				.filter(store -> store.getStatus() == StoreStatus.ACTIVE)
				.filter(store -> city == null || city.trim().isEmpty() || 
					(store.getCity() != null && store.getCity().equalsIgnoreCase(city.trim())))
				.collect(Collectors.toList());
		
		return filteredStores;
	}
}
