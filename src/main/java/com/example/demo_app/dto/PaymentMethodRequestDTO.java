package com.example.demo_app.dto;

import com.example.demo_app.entity.PaymentMethod.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodRequestDTO {

    @NotNull(message = "Payment method type is required")
    private PaymentMethodType type;

    @NotBlank(message = "Stripe token is required")
    private String stripeToken;

    private Boolean isDefault;

    // Card-specific fields
    private String cardBrand;
    private String cardLast4;
    private Integer cardExpiryMonth;
    private Integer cardExpiryYear;

    // Bank account-specific fields
    private String bankAccountLast4;
    private String bankAccountHolderName;
}
