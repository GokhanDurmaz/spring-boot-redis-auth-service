package com.example.demo_app.repository;

import com.example.demo_app.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByUserId(Long userId);
    Optional<Merchant> findByBusinessName(String businessName);
    Optional<Merchant> findByStripeAccountId(String stripeAccountId);
    List<Merchant> findByStatus(Merchant.MerchantStatus status);
}
