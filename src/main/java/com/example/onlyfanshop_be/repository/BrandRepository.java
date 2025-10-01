package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer> {
    Optional<Brand> findByBrandName(String brandName);
    boolean existsByBrandName(String brandName);
}