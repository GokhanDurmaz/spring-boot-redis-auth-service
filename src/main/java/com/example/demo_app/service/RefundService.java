package com.example.demo_app.service;

import com.example.demo_app.dto.RefundRequestDTO;
import com.example.demo_app.entity.Payment;
import com.example.demo_app.entity.PaymentEvent;
import com.example.demo_app.entity.Refund;
import com.example.demo_app.event.PaymentRefundedEvent;
import com.example.demo_app.exception.InvalidPaymentStateException;
import com.example.demo_app.exception.PaymentException;
import com.example.demo_app.repository.PaymentEventRepository;
import com.example.demo_app.repository.PaymentRepository;
import com.example.demo_app.repository.RefundRepository;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final StripeService stripeService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Refund refundPayment(Long paymentId, Long merchantId, RefundRequestDTO request) {
        log.info("Processing refund for payment: {} amount: {}", paymentId, request.getAmount());

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found with id: " + paymentId));

        // Verify merchant owns the store
        if (!payment.getStore().getMerchant().getId().equals(merchantId)) {
            throw new PaymentException("Unauthorized: You can only refund payments from your own store");
        }

        // Validate payment status
        if (payment.getStatus() != Payment.PaymentStatus.SUCCEEDED) {
            throw new InvalidPaymentStateException(
                    "Can only refund succeeded payments. Current status: " + payment.getStatus());
        }

        // Validate refund amount doesn't exceed payment amount
        if (request.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new PaymentException("Refund amount cannot exceed payment amount");
        }

        // Check if already fully refunded
        List<Refund> existingRefunds = refundRepository.findByPaymentId(paymentId);
        BigDecimal totalRefunded = existingRefunds.stream()
                .filter(r -> r.getStatus() == Refund.RefundStatus.SUCCEEDED)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRefunded.add(request.getAmount()).compareTo(payment.getAmount()) > 0) {
            throw new PaymentException("Total refunds would exceed payment amount");
        }

        try {
            // Create refund with Stripe
            com.stripe.model.Refund stripeRefund = stripeService.createRefund(
                    payment.getStripePaymentIntentId(),
                    request.getAmount().multiply(new BigDecimal("100")).longValue(),
                    request.getReason() != null ? request.getReason() : "requested_by_customer"
            );

            // Create refund record in database
            Refund refund = Refund.builder()
                    .payment(payment)
                    .stripeRefundId(stripeRefund.getId())
                    .amount(request.getAmount())
                    .status(Refund.RefundStatus.PENDING)
                    .reason(request.getReason())
                    .metadata(request.getMetadata())
                    .build();

            refund = refundRepository.save(refund);
            log.info("Refund created successfully: {}", refund.getId());

            // Check if refund succeeded immediately
            if (stripeRefund.getStatus() != null && stripeRefund.getStatus().equals("succeeded")) {
                refund.setStatus(Refund.RefundStatus.SUCCEEDED);
                refund.setRefundedAt(LocalDateTime.now());
                refund = refundRepository.save(refund);

                // Update payment status
                BigDecimal newTotalRefunded = totalRefunded.add(request.getAmount());
                if (newTotalRefunded.compareTo(payment.getAmount()) == 0) {
                    payment.setStatus(Payment.PaymentStatus.REFUNDED);
                } else {
                    payment.setStatus(Payment.PaymentStatus.PARTIAL_REFUNDED);
                }
                paymentRepository.save(payment);

                // Create event
                PaymentEvent event = PaymentEvent.builder()
                        .payment(payment)
                        .eventType(PaymentEvent.PaymentEventType.PAYMENT_REFUNDED)
                        .eventDetails("Payment refunded: " + request.getAmount())
                        .build();
                paymentEventRepository.save(event);

                // Publish event
                eventPublisher.publishEvent(new PaymentRefundedEvent(this, payment, refund));

                log.info("Refund succeeded immediately: {}", refund.getId());
            }

            return refund;

        } catch (StripeException e) {
            log.error("Failed to create refund with Stripe: {}", e.getMessage());
            throw new PaymentException("Failed to create refund: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void cancelPayment(Long paymentId, Long merchantId) {
        log.info("Canceling payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found with id: " + paymentId));

        // Verify merchant owns the store
        if (!payment.getStore().getMerchant().getId().equals(merchantId)) {
            throw new PaymentException("Unauthorized: You can only cancel payments from your own store");
        }

        // Can only cancel pending or processing payments
        if (payment.getStatus() != Payment.PaymentStatus.PENDING && 
            payment.getStatus() != Payment.PaymentStatus.PROCESSING) {
            throw new InvalidPaymentStateException(
                    "Can only cancel pending or processing payments. Current status: " + payment.getStatus());
        }

        try {
            // Cancel the payment intent with Stripe
            com.stripe.model.PaymentIntent intent = stripeService.retrievePaymentIntent(
                    payment.getStripePaymentIntentId());
            intent.cancel();

            // Update payment status
            payment.setStatus(Payment.PaymentStatus.CANCELLED);
            payment.setFailedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Create event
            PaymentEvent event = PaymentEvent.builder()
                    .payment(payment)
                    .eventType(PaymentEvent.PaymentEventType.PAYMENT_CANCELLED)
                    .eventDetails("Payment cancelled by merchant")
                    .build();
            paymentEventRepository.save(event);

            log.info("Payment cancelled successfully: {}", paymentId);

        } catch (StripeException e) {
            log.error("Failed to cancel payment with Stripe: {}", e.getMessage());
            throw new PaymentException("Failed to cancel payment: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Refund getRefund(Long refundId) {
        log.debug("Fetching refund: {}", refundId);
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new PaymentException("Refund not found with id: " + refundId));
    }

    @Transactional(readOnly = true)
    public List<Refund> getPaymentRefunds(Long paymentId) {
        log.debug("Fetching refunds for payment: {}", paymentId);
        return refundRepository.findByPaymentId(paymentId);
    }

    @Transactional
    public void handleRefundStatusUpdate(String stripeRefundId, String status) {
        log.info("Updating refund status: {} -> {}", stripeRefundId, status);

        Refund refund = refundRepository.findByStripeRefundId(stripeRefundId)
                .orElseThrow(() -> new PaymentException("Refund not found with Stripe ID: " + stripeRefundId));

        if ("succeeded".equals(status)) {
            refund.setStatus(Refund.RefundStatus.SUCCEEDED);
            refund.setRefundedAt(LocalDateTime.now());
            
            // Update payment status
            Payment payment = refund.getPayment();
            List<Refund> paymentRefunds = refundRepository.findByPaymentId(payment.getId());
            BigDecimal totalRefunded = paymentRefunds.stream()
                    .map(Refund::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalRefunded.compareTo(payment.getAmount()) == 0) {
                payment.setStatus(Payment.PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(Payment.PaymentStatus.PARTIAL_REFUNDED);
            }
            paymentRepository.save(payment);

            // Create event
            PaymentEvent event = PaymentEvent.builder()
                    .payment(payment)
                    .eventType(PaymentEvent.PaymentEventType.PAYMENT_REFUNDED)
                    .eventDetails("Refund succeeded: " + refund.getAmount())
                    .build();
            paymentEventRepository.save(event);

            // Publish event
            eventPublisher.publishEvent(new PaymentRefundedEvent(this, payment, refund));

        } else if ("failed".equals(status)) {
            refund.setStatus(Refund.RefundStatus.FAILED);
        } else if ("canceled".equals(status)) {
            refund.setStatus(Refund.RefundStatus.CANCELLED);
        }

        refundRepository.save(refund);
    }
}
