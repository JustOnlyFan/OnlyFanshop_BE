package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.GHNConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GHNConfigurationRepository extends JpaRepository<GHNConfiguration, Long> {
    
    /**
     * Tìm cấu hình đang active
     */
    Optional<GHNConfiguration> findFirstByIsActiveTrue();
    
    /**
     * Kiểm tra có cấu hình active không
     */
    boolean existsByIsActiveTrue();
}
