package com.example.demo_app.service;

import com.example.demo_app.entity.Payment;
import com.example.demo_app.entity.PaymentEvent;
import com.example.demo_app.exception.PaymentException;
import com.example.demo_app.repository.PaymentEventRepository;
import com.example.demo_app.repository.PaymentRepository;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PaymentService paymentService;

    @Transactional
    public void processWebhookEvent(Event event) {
        log.info("Processing Stripe webhook event: {}", event.getType());

        try {
            // Check if we've already processed this event
            Optional<PaymentEvent> existingEvent = paymentEventRepository.findByStripeEventId(event.getId());
            if (existingEvent.isPresent()) {
                log.debug("Event already processed: {}", event.getId());
                return;
            }

            // Handle different event types
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                case "payment_intent.canceled":
                    handlePaymentIntentCanceled(event);
                    break;
                case "charge.refunded":
                    handleChargeRefunded(event);
                    break;
                default:
                    log.debug("Unhandled webhook event type: {}", event.getType());
            }

        } catch (Exception e) {
            log.error("Error processing webhook event: {}", e.getMessage(), e);
            // Don't rethrow - we want webhook processing to be resilient
        }
    }

    private void handlePaymentIntentSucceeded(Event event) {
        log.info("Processing payment_intent.succeeded event");

        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new PaymentException("Failed to deserialize PaymentIntent"));

        // Find payment by Stripe payment intent ID
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId());

        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for Stripe intent: {}", paymentIntent.getId());
            return;
        }

        Payment payment = paymentOpt.get();

        // Update payment status
        paymentService.confirmPayment(payment.getId(), "succeeded", event.getId());

        // Log webhook event
        PaymentEvent webhookEvent = PaymentEvent.builder()
                .payment(payment)
                .eventType(PaymentEvent.PaymentEventType.WEBHOOK_PROCESSED)
                .stripeEventId(event.getId())
                .stripeEventType(event.getType())
                .eventDetails("Payment intent succeeded")
                .build();
        paymentEventRepository.save(webhookEvent);

        log.info("Payment succeeded: {}", payment.getId());
    }

    private void handlePaymentIntentFailed(Event event) {
        log.info("Processing payment_intent.payment_failed event");

        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new PaymentException("Failed to deserialize PaymentIntent"));

        // Find payment by Stripe payment intent ID
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId());

        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for Stripe intent: {}", paymentIntent.getId());
            return;
        }

        Payment payment = paymentOpt.get();

        // Get failure reason
        String failureReason = paymentIntent.getLastPaymentError() != null ? 
                paymentIntent.getLastPaymentError().getMessage() : "Unknown error";

        // Handle payment failure
        paymentService.handlePaymentFailure(payment.getId(), failureReason, event.getId());

        // Log webhook event
        PaymentEvent webhookEvent = PaymentEvent.builder()
                .payment(payment)
                .eventType(PaymentEvent.PaymentEventType.WEBHOOK_PROCESSED)
                .stripeEventId(event.getId())
                .stripeEventType(event.getType())
                .eventDetails("Payment intent failed: " + failureReason)
                .build();
        paymentEventRepository.save(webhookEvent);

        log.info("Payment failed: {}", payment.getId());
    }

    private void handlePaymentIntentCanceled(Event event) {
        log.info("Processing payment_intent.canceled event");

        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new PaymentException("Failed to deserialize PaymentIntent"));

        // Find payment by Stripe payment intent ID
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId());

        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for Stripe intent: {}", paymentIntent.getId());
            return;
        }

        Payment payment = paymentOpt.get();

        // Update payment status
        paymentService.confirmPayment(payment.getId(), "canceled", event.getId());

        // Log webhook event
        PaymentEvent webhookEvent = PaymentEvent.builder()
                .payment(payment)
                .eventType(PaymentEvent.PaymentEventType.WEBHOOK_PROCESSED)
                .stripeEventId(event.getId())
                .stripeEventType(event.getType())
                .eventDetails("Payment intent canceled")
                .build();
        paymentEventRepository.save(webhookEvent);

        log.info("Payment canceled: {}", payment.getId());
    }

    private void handleChargeRefunded(Event event) {
        log.info("Processing charge.refunded event");
        // This will be handled in the refund service
        log.debug("Charge refunded event logged for future processing");
    }
}
