package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.StaffDTO;
import com.example.onlyfanshop_be.dto.request.CreateStaffRequest;
import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.entity.Warehouse;
import com.example.onlyfanshop_be.enums.StoreStatus;
import com.example.onlyfanshop_be.enums.UserStatus;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.StoreLocationRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.repository.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
@Service
@Slf4j
public class StoreLocationService implements IStoreLocation {
    @Autowired
    private StoreLocationRepository storeLocationRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private StoreInventoryService storeInventoryService;
	@Autowired
	private WarehouseRepository warehouseRepository;
	@Autowired
	private StaffService staffService;
	@Autowired
	private IWarehouseService warehouseService;

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

		synchronizeStaffStatus(existing.getLocationID(), StoreStatus.CLOSED);

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
		List<StoreLocation> stores = storeInventoryService.getStoresWithProduct(productId);
		
		// Filter by city if provided, and only active stores
		List<StoreLocation> filteredStores = stores.stream()
				.filter(store -> store.getStatus() == StoreStatus.ACTIVE)
				.filter(store -> city == null || city.trim().isEmpty() || 
					(store.getCity() != null && store.getCity().equalsIgnoreCase(city.trim())))
				.collect(Collectors.toList());
		
		return filteredStores;
	}

	@Override
	@Transactional
	public StoreLocation createStoreWithStaffAndWarehouse(StoreLocation location, String staffPassword) {
		StoreLocation savedStore = createLocation(location);
		log.info("Created store location with ID: {}, name: {}", savedStore.getLocationID(), savedStore.getName());

		try {
			createStaffForStore(savedStore, staffPassword);
		} catch (Exception e) {
			log.error("Failed to create staff account for store ID: {} - Error: {}", 
					savedStore.getLocationID(), e.getMessage(), e);
			throw e;
		}

		try {
			createWarehouseForStore(savedStore);
		} catch (Exception e) {
			log.error("Failed to create warehouse for store ID: {} - Error: {}", 
					savedStore.getLocationID(), e.getMessage(), e);
			throw e;
		}

		synchronizeStaffStatus(savedStore.getLocationID(), savedStore.getStatus());

		return savedStore;
	}

	private StaffDTO createStaffForStore(StoreLocation store, String staffPassword) {
		log.info("Creating staff account for store ID: {}, name: {}", store.getLocationID(), store.getName());
		
		CreateStaffRequest staffRequest = new CreateStaffRequest();
		staffRequest.setStoreLocationId(store.getLocationID());

		String password = (staffPassword != null && !staffPassword.trim().isEmpty()) 
				? staffPassword 
				: "Staff@123";
		staffRequest.setPassword(password);

		StaffDTO staffDTO = staffService.createStaff(staffRequest);
		log.info("Successfully created staff account with ID: {} for store ID: {}", 
				staffDTO.getUserID(), store.getLocationID());
		
		return staffDTO;
	}

	private Warehouse createWarehouseForStore(StoreLocation store) {
		log.info("Creating warehouse for store ID: {}, name: {}", store.getLocationID(), store.getName());

		if (warehouseRepository.existsByStoreId(store.getLocationID())) {
			log.info("Warehouse already exists for store ID: {}", store.getLocationID());
			return warehouseRepository.findByStoreId(store.getLocationID())
					.orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
		}

		Warehouse warehouse = Warehouse.builder()
				.name("Kho " + store.getName())
				.type(WarehouseType.STORE)
				.storeId(store.getLocationID())
				.address(store.getAddress())
				.phone(store.getPhone())
				.build();
		
		Warehouse savedWarehouse = warehouseRepository.save(warehouse);
		log.info("Successfully created warehouse with ID: {} for store ID: {}", 
				savedWarehouse.getId(), store.getLocationID());

		// Tự động thêm tất cả sản phẩm vào kho mới với số lượng = 0 và isEnabled = true
		try {
			warehouseService.addAllProductsToWarehouse(savedWarehouse.getId());
			log.info("Successfully added all products to warehouse ID: {}", savedWarehouse.getId());
		} catch (Exception e) {
			log.error("Failed to add products to warehouse ID: {} - Error: {}", 
					savedWarehouse.getId(), e.getMessage(), e);
		}
		
		return savedWarehouse;
	}

}
