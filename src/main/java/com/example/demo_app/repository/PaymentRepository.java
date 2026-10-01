package com.example.demo_app.repository;

import com.example.demo_app.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    List<Payment> findByCustomerId(Long customerId);
    Page<Payment> findByCustomerId(Long customerId, Pageable pageable);
    List<Payment> findByStoreId(Long storeId);
    Page<Payment> findByStoreId(Long storeId, Pageable pageable);
    List<Payment> findByStatus(Payment.PaymentStatus status);
    List<Payment> findByStatusAndNextRetryTimeIsNotNull(Payment.PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.store.id = :storeId AND p.status = :status")
    Page<Payment> findByStoreIdAndStatus(@Param("storeId") Long storeId, @Param("status") Payment.PaymentStatus status, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.customerId = :customerId AND p.status = :status")
    Page<Payment> findByCustomerIdAndStatus(@Param("customerId") Long customerId, @Param("status") Payment.PaymentStatus status, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.customerId = :customerId AND p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findByCustomerIdAndDateRange(@Param("customerId") Long customerId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
