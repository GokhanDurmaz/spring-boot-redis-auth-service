package com.example.demo_app.event.listener;

import com.example.demo_app.event.PaymentCreatedEvent;
import com.example.demo_app.event.PaymentFailedEvent;
import com.example.demo_app.event.PaymentRefundedEvent;
import com.example.demo_app.event.PaymentSucceededEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class PaymentNotificationListener {

    /**
     * Send notification when payment is created
     * In a real application, this would send an email or push notification
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onPaymentCreated(PaymentCreatedEvent event) {
        log.info("Sending payment created notification for payment: {}", event.getPayment().getId());
        // TODO: Send email/SMS notification to customer and merchant
        // notificationService.sendPaymentCreatedNotification(event.getPayment());
    }

    /**
     * Send notification when payment succeeds
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Sending payment succeeded notification for payment: {}", event.getPayment().getId());
        // TODO: Send email/SMS confirmation to customer and merchant
        // notificationService.sendPaymentSuccessNotification(event.getPayment());
    }

    /**
     * Send notification when payment fails
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("Sending payment failed notification for payment: {}", event.getPayment().getId());
        // TODO: Send email/SMS failure notification to customer
        // notificationService.sendPaymentFailureNotification(event.getPayment(), event.getFailureReason());
    }

    /**
     * Send notification when payment is refunded
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        log.info("Sending refund notification for payment: {}, refund: {}", 
                event.getPayment().getId(), 
                event.getRefund().getId());
        // TODO: Send email/SMS refund confirmation to customer
        // notificationService.sendRefundNotification(event.getPayment(), event.getRefund());
    }
}
