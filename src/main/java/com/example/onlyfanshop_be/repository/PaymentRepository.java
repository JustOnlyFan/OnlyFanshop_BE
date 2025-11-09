package com.example.onlyfanshop_be.repository;


import com.example.onlyfanshop_be.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByProviderTxnId(String providerTxnId);
    
    // Legacy method for backward compatibility
    @Deprecated
    default boolean existsByTransactionCode(String paymentCode) {
        return existsByProviderTxnId(paymentCode);
    }
}

