package com.example.demo_app.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentEventType eventType;

    @Column(length = 500)
    private String eventDetails;

    @Column
    private String stripeEventId;

    @Column
    private String stripeEventType;

    @Column(length = 2000)
    private String rawEventData;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum PaymentEventType {
        PAYMENT_CREATED,
        PAYMENT_PROCESSING,
        PAYMENT_SUCCEEDED,
        PAYMENT_FAILED,
        PAYMENT_CANCELLED,
        PAYMENT_REFUNDED,
        PAYMENT_PARTIAL_REFUNDED,
        PAYMENT_RETRY_ATTEMPT,
        PAYMENT_RETRY_EXHAUSTED,
        WEBHOOK_RECEIVED,
        WEBHOOK_PROCESSED,
        WEBHOOK_FAILED
    }
}
