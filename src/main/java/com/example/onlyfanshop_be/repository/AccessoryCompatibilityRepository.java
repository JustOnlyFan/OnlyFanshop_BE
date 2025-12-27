package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.AccessoryCompatibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessoryCompatibilityRepository extends JpaRepository<AccessoryCompatibility, Long> {

    List<AccessoryCompatibility> findByAccessoryProductId(Long accessoryProductId);

    @Query("SELECT ac FROM AccessoryCompatibility ac " +
           "LEFT JOIN FETCH ac.compatibleFanType " +
           "LEFT JOIN FETCH ac.compatibleBrand " +
           "WHERE ac.accessoryProductId = :accessoryProductId")
    List<AccessoryCompatibility> findByAccessoryProductIdWithDetails(@Param("accessoryProductId") Long accessoryProductId);

    List<AccessoryCompatibility> findByCompatibleFanTypeId(Integer compatibleFanTypeId);

    @Query("SELECT DISTINCT ac.accessoryProductId FROM AccessoryCompatibility ac WHERE ac.compatibleFanTypeId = :fanTypeId")
    List<Long> findAccessoryProductIdsByCompatibleFanTypeId(@Param("fanTypeId") Integer compatibleFanTypeId);

    List<AccessoryCompatibility> findByCompatibleBrandId(Integer compatibleBrandId);

    @Query("SELECT DISTINCT ac.accessoryProductId FROM AccessoryCompatibility ac WHERE ac.compatibleBrandId = :brandId")
    List<Long> findAccessoryProductIdsByCompatibleBrandId(@Param("brandId") Integer compatibleBrandId);

    @Query("SELECT ac FROM AccessoryCompatibility ac WHERE ac.compatibleModel LIKE %:modelPattern%")
    List<AccessoryCompatibility> findByCompatibleModelContaining(@Param("modelPattern") String modelPattern);

    @Query("SELECT DISTINCT ac.accessoryProductId FROM AccessoryCompatibility ac " +
           "WHERE ac.compatibleFanTypeId = :fanTypeId AND ac.compatibleBrandId = :brandId")
    List<Long> findAccessoryProductIdsByFanTypeAndBrand(@Param("fanTypeId") Integer fanTypeId, @Param("brandId") Integer brandId);

    boolean existsByAccessoryProductId(Long accessoryProductId);

    boolean existsByAccessoryProductIdAndCompatibleFanTypeId(Long accessoryProductId, Integer fanTypeId);

    boolean existsByAccessoryProductIdAndCompatibleBrandId(Long accessoryProductId, Integer brandId);

    void deleteByAccessoryProductId(Long accessoryProductId);

    long countByAccessoryProductId(Long accessoryProductId);

    @Query("SELECT COUNT(DISTINCT ac.accessoryProductId) FROM AccessoryCompatibility ac WHERE ac.compatibleFanTypeId = :fanTypeId")
    long countAccessoriesByCompatibleFanTypeId(@Param("fanTypeId") Integer fanTypeId);
}
