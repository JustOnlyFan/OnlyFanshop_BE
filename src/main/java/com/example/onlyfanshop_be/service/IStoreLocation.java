package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.StaffDTO;
import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.entity.Warehouse;

import java.util.List;

public interface IStoreLocation {
    StoreLocation updateLocation(int id, StoreLocation location);
    StoreLocation getLocationById(int id);
    List<StoreLocation> getAllLocations();
    StoreLocation createLocation(StoreLocation location);
    void deleteLocation(int id);
    void synchronizeStaffStatus(int storeId, com.example.onlyfanshop_be.enums.StoreStatus status);
    List<StoreLocation> getStoresWithProduct(Long productId, String city, String district);
    
    /**
     * Create a store with associated Staff account and Store_Warehouse
     * Requirements: 3.2 - WHEN a Store is created THEN the System SHALL automatically create one Staff account associated with that Store
     * Requirements: 3.3 - WHEN a Store is created THEN the System SHALL automatically create one Store_Warehouse associated with that Store
     * Requirements: 3.4 - WHEN a Store is created THEN the Store_Warehouse SHALL be empty with no Inventory_Items
     * 
     * @param location The store location to create
     * @param staffPassword Optional password for the staff account (uses default if null)
     * @return The created store location
     */
    StoreLocation createStoreWithStaffAndWarehouse(StoreLocation location, String staffPassword);
    
    /**
     * Get the Staff account associated with a store
     * @param storeId The store ID
     * @return StaffDTO or null if no staff exists
     */
    StaffDTO getStoreStaff(int storeId);
    
    /**
     * Get the Warehouse associated with a store
     * @param storeId The store ID
     * @return Warehouse or null if no warehouse exists
     */
    Warehouse getStoreWarehouse(int storeId);
}
