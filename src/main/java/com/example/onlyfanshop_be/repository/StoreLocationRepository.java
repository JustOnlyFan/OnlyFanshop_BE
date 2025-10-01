package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.StoreLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreLocationRepository extends JpaRepository<StoreLocation, Integer> {
}

