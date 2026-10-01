package com.example.demo_app.dto;

import com.example.demo_app.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodResponseDTO {

    private Long id;

    private String type;

    private String cardBrand;

    private String cardLast4;

    private Integer cardExpiryMonth;

    private Integer cardExpiryYear;

    private String bankAccountLast4;

    private String bankAccountHolderName;

    private Boolean isDefault;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static PaymentMethodResponseDTO fromEntity(PaymentMethod method) {
        return PaymentMethodResponseDTO.builder()
                .id(method.getId())
                .type(method.getType().toString())
                .cardBrand(method.getCardBrand())
                .cardLast4(method.getCardLast4())
                .cardExpiryMonth(method.getCardExpiryMonth())
                .cardExpiryYear(method.getCardExpiryYear())
                .bankAccountLast4(method.getBankAccountLast4())
                .bankAccountHolderName(method.getBankAccountHolderName())
                .isDefault(method.getIsDefault())
                .isActive(method.getIsActive())
                .createdAt(method.getCreatedAt())
                .updatedAt(method.getUpdatedAt())
                .build();
    }
}
