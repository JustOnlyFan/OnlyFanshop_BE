package com.example.onlyfanshop_be.repository;


import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);

    @Query("SELECT DISTINCT u FROM User u WHERE u.userID IN " +
           "(SELECT DISTINCT cm.sender.userID FROM ChatMessage cm WHERE cm.receiver.userID = :adminId) " +
           "OR u.userID IN " +
           "(SELECT DISTINCT cm.receiver.userID FROM ChatMessage cm WHERE cm.sender.userID = :adminId)")
    List<User> findUsersWhoChattedWithAdmin(@Param("adminId") Integer adminId);

}

