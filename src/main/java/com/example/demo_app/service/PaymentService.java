package com.example.demo_app.service;

import com.example.demo_app.dto.PaymentRequestDTO;
import com.example.demo_app.entity.Payment;
import com.example.demo_app.entity.PaymentEvent;
import com.example.demo_app.entity.PaymentMethod;
import com.example.demo_app.entity.Store;
import com.example.demo_app.event.PaymentCreatedEvent;
import com.example.demo_app.exception.InvalidPaymentStateException;
import com.example.demo_app.exception.PaymentException;
import com.example.demo_app.exception.PaymentMethodNotFoundException;
import com.example.demo_app.exception.StoreNotFoundException;
import com.example.demo_app.repository.PaymentEventRepository;
import com.example.demo_app.repository.PaymentMethodRepository;
import com.example.demo_app.repository.PaymentRepository;
import com.example.demo_app.repository.StoreRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final StoreRepository storeRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final StripeService stripeService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Payment createPayment(Long customerId, PaymentRequestDTO request) {
        log.info("Creating payment for customer: {} to store: {}", customerId, request.getStoreId());

        // Validate store exists
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new StoreNotFoundException(
                        "Store not found with id: " + request.getStoreId()));

        // Validate payment method exists and belongs to customer
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(
                request.getPaymentMethodId(), customerId)
                .orElseThrow(() -> new PaymentMethodNotFoundException(
                        "Payment method not found with id: " + request.getPaymentMethodId()));

        // Generate idempotency key to prevent duplicate charges
        String idempotencyKey = UUID.randomUUID().toString();

        try {
            // Create payment intent with Stripe
            PaymentIntent paymentIntent = stripeService.createPaymentIntent(
                    "customer_" + customerId,
                    paymentMethod.getStripePaymentMethodId(),
                    request.getAmount(),
                    request.getCurrency(),
                    request.getDescription(),
                    idempotencyKey
            );

            // Create payment record in database
            Payment payment = Payment.builder()
                    .store(store)
                    .customerId(customerId)
                    .stripePaymentIntentId(paymentIntent.getId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status(Payment.PaymentStatus.PENDING)
                    .paymentMethodId(paymentMethod.getId())
                    .stripeCustomerId("customer_" + customerId)
                    .description(request.getDescription())
                    .metadata(request.getMetadata())
                    .idempotencyKey(idempotencyKey)
                    .retryCount(0)
                    .build();

            payment = paymentRepository.save(payment);
            log.info("Payment created successfully: {}", payment.getId());

            // Create payment event for audit
            PaymentEvent event = PaymentEvent.builder()
                    .payment(payment)
                    .eventType(PaymentEvent.PaymentEventType.PAYMENT_CREATED)
                    .eventDetails("Payment created with Stripe intent: " + paymentIntent.getId())
                    .stripeEventType("payment.created")
                    .build();
            paymentEventRepository.save(event);

            // Publish event for listeners
            eventPublisher.publishEvent(new PaymentCreatedEvent(this, payment));

            return payment;

        } catch (StripeException e) {
            log.error("Failed to create payment with Stripe: {}", e.getMessage());
            throw new PaymentException("Failed to create payment: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Payment getPayment(Long paymentId) {
        log.debug("Fetching payment: {}", paymentId);
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found with id: " + paymentId));
    }

    @Transactional
    public void confirmPayment(Long paymentId, String status, String stripeEventId) {
        log.info("Confirming payment: {} with status: {}", paymentId, status);

        Payment payment = getPayment(paymentId);

        // Update payment status based on Stripe status
        if ("succeeded".equals(status)) {
            payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
            payment.setSucceededAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);

            // Create event
            PaymentEvent event = PaymentEvent.builder()
                    .payment(payment)
                    .eventType(PaymentEvent.PaymentEventType.PAYMENT_SUCCEEDED)
                    .stripeEventId(stripeEventId)
                    .stripeEventType("payment_intent.succeeded")
                    .build();
            paymentEventRepository.save(event);

            log.info("Payment succeeded: {}", paymentId);

        } else if ("requires_payment_method".equals(status) || "requires_action".equals(status)) {
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

        } else if ("canceled".equals(status)) {
            payment.setStatus(Payment.PaymentStatus.CANCELLED);
            payment.setFailedAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);

            PaymentEvent event = PaymentEvent.builder()
                    .payment(payment)
                    .eventType(PaymentEvent.PaymentEventType.PAYMENT_CANCELLED)
                    .stripeEventId(stripeEventId)
                    .stripeEventType("payment_intent.canceled")
                    .build();
            paymentEventRepository.save(event);

            log.info("Payment cancelled: {}", paymentId);
        }
    }

    @Transactional
    public void handlePaymentFailure(Long paymentId, String failureReason, String stripeEventId) {
        log.error("Handling payment failure for payment: {}", paymentId);

        Payment payment = getPayment(paymentId);

        if (payment.getStatus() == Payment.PaymentStatus.SUCCEEDED) {
            throw new InvalidPaymentStateException("Cannot fail a payment that has already succeeded");
        }

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        payment.setFailedAt(java.time.LocalDateTime.now());
        
        // Initialize retry count and schedule first retry
        if (payment.getRetryCount() == null) {
            payment.setRetryCount(0);
        }
        
        // Schedule retry after 1 second (exponential backoff will handle subsequent retries)
        payment.setNextRetryTime(java.time.LocalDateTime.now().plusSeconds(1));
        
        paymentRepository.save(payment);

        // Create event
        PaymentEvent event = PaymentEvent.builder()
                .payment(payment)
                .eventType(PaymentEvent.PaymentEventType.PAYMENT_FAILED)
                .eventDetails("Payment failed: " + failureReason)
                .stripeEventId(stripeEventId)
                .stripeEventType("payment_intent.payment_failed")
                .build();
        paymentEventRepository.save(event);

        log.info("Payment marked as failed and scheduled for retry: {}", paymentId);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByStripeIntentId(String stripePaymentIntentId) {
        log.debug("Fetching payment by Stripe intent: {}", stripePaymentIntentId);
        return paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new PaymentException(
                        "Payment not found with Stripe intent: " + stripePaymentIntentId));
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByIdempotencyKey(String idempotencyKey) {
        log.debug("Fetching payment by idempotency key: {}", idempotencyKey);
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new PaymentException(
                        "Payment not found with idempotency key: " + idempotencyKey));
    }

    @Transactional(readOnly = true)
    public Page<Payment> listCustomerPayments(Long customerId, Pageable pageable, String status) {
        log.debug("Listing payments for customer: {} with status filter: {}", customerId, status);

        if (status != null && !status.isEmpty()) {
            try {
                Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
                return paymentRepository.findByCustomerIdAndStatus(customerId, paymentStatus, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid payment status filter: {}", status);
            }
        }

        return paymentRepository.findByCustomerId(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Payment> listStorePayments(Long storeId, Pageable pageable, String status) {
        log.debug("Listing payments for store: {} with status filter: {}", storeId, status);

        if (status != null && !status.isEmpty()) {
            try {
                Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
                return paymentRepository.findByStoreIdAndStatus(storeId, paymentStatus, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid payment status filter: {}", status);
            }
        }

        return paymentRepository.findByStoreId(storeId, pageable);
    }
}
