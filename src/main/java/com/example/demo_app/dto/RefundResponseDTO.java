package com.example.demo_app.dto;

import com.example.demo_app.entity.Refund;
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
public class RefundResponseDTO {

    private Long id;

    private Long paymentId;

    private BigDecimal amount;

    private String status;

    private String reason;

    private LocalDateTime refundedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static RefundResponseDTO fromEntity(Refund refund) {
        return RefundResponseDTO.builder()
                .id(refund.getId())
                .paymentId(refund.getPayment().getId())
                .amount(refund.getAmount())
                .status(refund.getStatus().toString())
                .reason(refund.getReason())
                .refundedAt(refund.getRefundedAt())
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt())
                .build();
    }
}
