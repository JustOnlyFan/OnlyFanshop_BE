package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.StoreLocation;

import java.util.List;

public interface IStoreLocation {
    StoreLocation updateLocation(int id, StoreLocation location);
    StoreLocation getLocationById(int id);
    List<StoreLocation> getAllLocations();
    StoreLocation createLocation(StoreLocation location);
    void deleteLocation(int id);
    void synchronizeStaffStatus(int storeId, com.example.onlyfanshop_be.enums.StoreStatus status);
}
