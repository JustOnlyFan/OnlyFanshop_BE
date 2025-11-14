package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Role;
import com.example.onlyfanshop_be.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByRoleId(Byte roleId);
    List<User> findByRole(Role role);
    
    @Query("SELECT u FROM User u " +
            "WHERE (:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR u.phone LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:roleId IS NULL OR u.roleId = :roleId)")
    Page<User> searchUsers(@Param("keyword") String keyword, @Param("roleId") Byte roleId, Pageable pageable);
    
    List<User> findByStoreLocationId(Integer storeLocationId);
    
    Page<User> findByRoleId(Byte roleId, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.roleId = :roleId AND (:storeLocationId IS NULL OR u.storeLocationId = :storeLocationId)")
    Page<User> findByRoleIdAndStoreLocationId(@Param("roleId") Byte roleId, @Param("storeLocationId") Integer storeLocationId, Pageable pageable);

    @Query("SELECT u FROM User u LEFT JOIN u.role r " +
            "WHERE (:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR u.phone LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:storeLocationId IS NULL OR u.storeLocationId = :storeLocationId) " +
            "AND (r IS NULL OR LOWER(r.name) NOT IN :excludedRoles)")
    Page<User> findAccountsExcludingRoles(@Param("keyword") String keyword,
                                          @Param("storeLocationId") Integer storeLocationId,
                                          @Param("excludedRoles") List<String> excludedRoles,
                                          Pageable pageable);
}

