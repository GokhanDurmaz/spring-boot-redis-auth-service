package com.example.demo_app.dto;

import com.example.demo_app.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {

    private Long id;

    private Long storeId;

    private Long customerId;

    private BigDecimal amount;

    private String currency;

    private String status;

    private String description;

    private String stripePaymentIntentId;

    private LocalDateTime succeededAt;

    private LocalDateTime failedAt;

    private String failureReason;

    private Integer retryCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static PaymentResponseDTO fromEntity(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .storeId(payment.getStore().getId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().toString())
                .description(payment.getDescription())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .succeededAt(payment.getSucceededAt())
                .failedAt(payment.getFailedAt())
                .failureReason(payment.getFailureReason())
                .retryCount(payment.getRetryCount())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
