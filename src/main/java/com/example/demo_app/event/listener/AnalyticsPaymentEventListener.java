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

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class AnalyticsPaymentEventListener {

    private final AtomicLong totalPaymentsCreated = new AtomicLong(0);
    private final AtomicLong totalPaymentsSucceeded = new AtomicLong(0);
    private final AtomicLong totalPaymentsFailed = new AtomicLong(0);
    private final AtomicLong totalPaymentsRefunded = new AtomicLong(0);
    private final AtomicReference<BigDecimal> totalAmountProcessed = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> totalAmountRefunded = new AtomicReference<>(BigDecimal.ZERO);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCreated(PaymentCreatedEvent event) {
        totalPaymentsCreated.incrementAndGet();
        totalAmountProcessed.updateAndGet(current -> current.add(event.getPayment().getAmount()));
        logAnalytics("Payment Created");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        totalPaymentsSucceeded.incrementAndGet();
        logAnalytics("Payment Succeeded");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentFailed(PaymentFailedEvent event) {
        totalPaymentsFailed.incrementAndGet();
        logAnalytics("Payment Failed");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        totalPaymentsRefunded.incrementAndGet();
        totalAmountRefunded.updateAndGet(current -> current.add(event.getRefund().getAmount()));
        logAnalytics("Payment Refunded");
    }

    private void logAnalytics(String eventType) {
        log.info("Analytics - Event: {}, Created: {}, Succeeded: {}, Failed: {}, Refunded: {}, Total Processed: {}, Total Refunded: {}", 
                eventType,
                totalPaymentsCreated.get(),
                totalPaymentsSucceeded.get(),
                totalPaymentsFailed.get(),
                totalPaymentsRefunded.get(),
                totalAmountProcessed.get(),
                totalAmountRefunded.get());
    }

    public AnalyticsSnapshot getSnapshot() {
        return AnalyticsSnapshot.builder()
                .totalPaymentsCreated(totalPaymentsCreated.get())
                .totalPaymentsSucceeded(totalPaymentsSucceeded.get())
                .totalPaymentsFailed(totalPaymentsFailed.get())
                .totalPaymentsRefunded(totalPaymentsRefunded.get())
                .totalAmountProcessed(totalAmountProcessed.get())
                .totalAmountRefunded(totalAmountRefunded.get())
                .successRate(totalPaymentsCreated.get() > 0 ? 
                        (totalPaymentsSucceeded.get() * 100.0 / totalPaymentsCreated.get()) : 0.0)
                .build();
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AnalyticsSnapshot {
        private long totalPaymentsCreated;
        private long totalPaymentsSucceeded;
        private long totalPaymentsFailed;
        private long totalPaymentsRefunded;
        private BigDecimal totalAmountProcessed;
        private BigDecimal totalAmountRefunded;
        private double successRate;
    }
}
