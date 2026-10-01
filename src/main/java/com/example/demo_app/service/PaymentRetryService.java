package com.example.demo_app.service;

import com.example.demo_app.entity.Payment;
import com.example.demo_app.entity.PaymentEvent;
import com.example.demo_app.exception.InvalidPaymentStateException;
import com.example.demo_app.exception.PaymentException;
import com.example.demo_app.repository.PaymentEventRepository;
import com.example.demo_app.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRetryService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final StripeService stripeService;

    @Value("${payment.retry.max-attempts:5}")
    private int maxRetryAttempts;

    @Value("${payment.retry.initial-delay-seconds:1}")
    private int initialDelaySeconds;

    @Value("${payment.retry.max-delay-seconds:300}")
    private int maxDelaySeconds;

    /**
     * Scheduled task that runs every 30 seconds to check for failed payments that need retrying
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 60000)
    @Transactional
    public void processFailedPaymentsForRetry() {
        log.debug("Processing failed payments for retry");

        // Find all failed payments with nextRetryTime set and that time is now or in the past
        List<Payment> paymentsToRetry = paymentRepository.findByStatusAndNextRetryTimeIsNotNull(
                Payment.PaymentStatus.FAILED);

        LocalDateTime now = LocalDateTime.now();
        for (Payment payment : paymentsToRetry) {
            if (payment.getNextRetryTime() != null && payment.getNextRetryTime().isBefore(now)) {
                retryPayment(payment);
            }
        }
    }

    /**
     * Retry a failed payment with exponential backoff
     */
    @Transactional
    public void retryPayment(Payment payment) {
        log.info("Retrying payment: {} (attempt: {})", payment.getId(), payment.getRetryCount() + 1);

        // Check if we've exceeded max retry attempts
        if (payment.getRetryCount() >= maxRetryAttempts) {
            log.error("Payment retry exhausted for payment: {}", payment.getId());
            handleRetryExhausted(payment);
            return;
        }

        try {
            // Retrieve the payment intent from Stripe
            PaymentIntent intent = stripeService.retrievePaymentIntent(payment.getStripePaymentIntentId());

            // Attempt to confirm the payment intent
            if (intent.getStatus() != null) {
                String status = intent.getStatus().toString();
                
                if ("succeeded".equals(status)) {
                    // Payment succeeded, update local record
                    payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                    payment.setSucceededAt(LocalDateTime.now());
                    payment.setNextRetryTime(null);
                    paymentRepository.save(payment);

                    // Create event
                    PaymentEvent event = PaymentEvent.builder()
                            .payment(payment)
                            .eventType(PaymentEvent.PaymentEventType.PAYMENT_SUCCEEDED)
                            .eventDetails("Payment succeeded on retry attempt " + (payment.getRetryCount() + 1))
                            .build();
                    paymentEventRepository.save(event);

                    log.info("Payment succeeded on retry: {}", payment.getId());
                    return;
                }
            }

            // If not succeeded, increment retry count and schedule next retry
            payment.setRetryCount(payment.getRetryCount() + 1);
            LocalDateTime nextRetryTime = calculateNextRetryTime(payment.getRetryCount());
            payment.setNextRetryTime(nextRetryTime);
            paymentRepository.save(payment);

            // Create retry event
            PaymentEvent event = PaymentEvent.builder()
                    .payment(payment)
                    .eventType(PaymentEvent.PaymentEventType.PAYMENT_RETRY_ATTEMPT)
                    .eventDetails("Payment retry attempt " + payment.getRetryCount() + 
                                " scheduled for " + nextRetryTime)
                    .build();
            paymentEventRepository.save(event);

            log.info("Payment retry scheduled for: {} next attempt at: {}", 
                    payment.getId(), nextRetryTime);

        } catch (StripeException e) {
            log.error("Error during payment retry: {}", e.getMessage());
            payment.setRetryCount(payment.getRetryCount() + 1);
            
            if (payment.getRetryCount() < maxRetryAttempts) {
                LocalDateTime nextRetryTime = calculateNextRetryTime(payment.getRetryCount());
                payment.setNextRetryTime(nextRetryTime);
                paymentRepository.save(payment);

                PaymentEvent event = PaymentEvent.builder()
                        .payment(payment)
                        .eventType(PaymentEvent.PaymentEventType.PAYMENT_RETRY_ATTEMPT)
                        .eventDetails("Payment retry attempt " + payment.getRetryCount() + 
                                    " failed: " + e.getMessage() + 
                                    ", scheduled for " + nextRetryTime)
                        .build();
                paymentEventRepository.save(event);
            } else {
                handleRetryExhausted(payment);
            }
        }
    }

    /**
     * Handle when payment retry attempts are exhausted
     */
    @Transactional
    public void handleRetryExhausted(Payment payment) {
        log.error("Payment retry exhausted, marking as failed: {}", payment.getId());

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setFailedAt(LocalDateTime.now());
        payment.setFailureReason("Payment retry attempts exhausted after " + maxRetryAttempts + " attempts");
        payment.setNextRetryTime(null);
        paymentRepository.save(payment);

        // Create event
        PaymentEvent event = PaymentEvent.builder()
                .payment(payment)
                .eventType(PaymentEvent.PaymentEventType.PAYMENT_RETRY_EXHAUSTED)
                .eventDetails("Payment retry exhausted after " + maxRetryAttempts + " attempts")
                .build();
        paymentEventRepository.save(event);
    }

    /**
     * Schedule payment for retry after a failed attempt
     */
    @Transactional
    public void scheduleRetry(Long paymentId) {
        log.info("Scheduling retry for payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found with id: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.FAILED) {
            throw new InvalidPaymentStateException(
                    "Can only retry failed payments. Current status: " + payment.getStatus());
        }

        // Reset retry count if this is the first retry
        if (payment.getRetryCount() == null) {
            payment.setRetryCount(0);
        }

        LocalDateTime nextRetryTime = calculateNextRetryTime(payment.getRetryCount() + 1);
        payment.setNextRetryTime(nextRetryTime);
        paymentRepository.save(payment);

        log.info("Payment scheduled for retry at: {}", nextRetryTime);
    }

    /**
     * Calculate next retry time using exponential backoff strategy
     * Formula: min(initial_delay * 2^attempt, max_delay)
     */
    private LocalDateTime calculateNextRetryTime(int attemptNumber) {
        // Calculate delay in seconds using exponential backoff
        long delayInSeconds = (long) (initialDelaySeconds * Math.pow(2, attemptNumber - 1));
        
        // Cap at max delay
        delayInSeconds = Math.min(delayInSeconds, maxDelaySeconds);
        
        return LocalDateTime.now().plusSeconds(delayInSeconds);
    }

    /**
     * Get retry schedule information for a payment
     */
    @Transactional(readOnly = true)
    public PaymentRetryInfo getRetryInfo(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found with id: " + paymentId));

        return PaymentRetryInfo.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus().toString())
                .currentRetryCount(payment.getRetryCount() != null ? payment.getRetryCount() : 0)
                .maxRetryAttempts(maxRetryAttempts)
                .nextRetryTime(payment.getNextRetryTime())
                .build();
    }

    /**
     * DTO for retry information
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class PaymentRetryInfo {
        private Long paymentId;
        private String status;
        private int currentRetryCount;
        private int maxRetryAttempts;
        private LocalDateTime nextRetryTime;
    }
}
