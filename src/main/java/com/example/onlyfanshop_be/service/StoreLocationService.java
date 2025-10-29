package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.StoreLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class StoreLocationService implements IStoreLocation {
    @Autowired
    private StoreLocationRepository storeLocationRepository;

    @Override
    public List<StoreLocation> getAllLocations() {
        return storeLocationRepository.findAll();
    }

    @Override
    public StoreLocation getLocationById(int id) {
        return storeLocationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
    }

    @Override
    public StoreLocation createLocation(StoreLocation storeLocation) {
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
        existing.setPhone(newLocation.getPhone());
        existing.setOpeningHours(newLocation.getOpeningHours());
        return storeLocationRepository.save(existing);
    }

    @Override
    public void deleteLocation(int id) {
        if (!storeLocationRepository.existsById(id)) {
            throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
        }
        storeLocationRepository.deleteById(id);
    }
}
