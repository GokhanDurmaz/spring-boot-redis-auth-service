package com.example.demo_app.repository;

import com.example.demo_app.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    List<PaymentEvent> findByPaymentId(Long paymentId);
    List<PaymentEvent> findByEventType(PaymentEvent.PaymentEventType eventType);
    Optional<PaymentEvent> findByStripeEventId(String stripeEventId);
}
