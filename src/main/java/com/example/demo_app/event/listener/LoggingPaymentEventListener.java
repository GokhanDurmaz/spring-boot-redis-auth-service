package com.example.demo_app.event.listener;

import com.example.demo_app.event.PaymentCreatedEvent;
import com.example.demo_app.event.PaymentFailedEvent;
import com.example.demo_app.event.PaymentRefundedEvent;
import com.example.demo_app.event.PaymentSucceededEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class LoggingPaymentEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCreated(PaymentCreatedEvent event) {
        log.info("Payment Created Event - Payment ID: {}, Amount: {} {}, Store: {}", 
                event.getPayment().getId(),
                event.getPayment().getAmount(),
                event.getPayment().getCurrency(),
                event.getPayment().getStore().getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Payment Succeeded Event - Payment ID: {}, Amount: {} {}, Customer: {}", 
                event.getPayment().getId(),
                event.getPayment().getAmount(),
                event.getPayment().getCurrency(),
                event.getPayment().getCustomerId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("Payment Failed Event - Payment ID: {}, Reason: {}, Attempt Count: {}", 
                event.getPayment().getId(),
                event.getFailureReason(),
                event.getPayment().getRetryCount());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        log.info("Payment Refunded Event - Payment ID: {}, Refund ID: {}, Amount: {}", 
                event.getPayment().getId(),
                event.getRefund().getId(),
                event.getRefund().getAmount());
    }
}
