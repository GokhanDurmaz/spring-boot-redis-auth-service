package com.example.demo_app.repository;

import com.example.demo_app.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByUserId(Long userId);
    List<PaymentMethod> findByUserIdAndIsActive(Long userId, Boolean isActive);
    Optional<PaymentMethod> findByStripePaymentMethodId(String stripePaymentMethodId);
    Optional<PaymentMethod> findByIdAndUserId(Long id, Long userId);
}
