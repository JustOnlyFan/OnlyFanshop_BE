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
            "AND (:role IS NULL OR u.roleId = :roleId)")
    Page<User> searchUsers(@Param("keyword") String keyword, @Param("roleId") Byte roleId, Pageable pageable);
}

