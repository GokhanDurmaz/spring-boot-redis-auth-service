package com.example.demo_app.dto;

import com.example.demo_app.entity.Merchant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantResponseDTO {

    private Long id;

    private Long userId;

    private String businessName;

    private String businessEmail;

    private String businessPhone;

    private String businessAddress;

    private String businessCity;

    private String businessState;

    private String businessZip;

    private String businessCountry;

    private String businessDescription;

    private String taxId;

    private String status;

    private String stripeAccountId;

    private String stripeVerificationStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static MerchantResponseDTO fromEntity(Merchant merchant) {
        return MerchantResponseDTO.builder()
                .id(merchant.getId())
                .userId(merchant.getUserId())
                .businessName(merchant.getBusinessName())
                .businessEmail(merchant.getBusinessEmail())
                .businessPhone(merchant.getBusinessPhone())
                .businessAddress(merchant.getBusinessAddress())
                .businessCity(merchant.getBusinessCity())
                .businessState(merchant.getBusinessState())
                .businessZip(merchant.getBusinessZip())
                .businessCountry(merchant.getBusinessCountry())
                .businessDescription(merchant.getBusinessDescription())
                .taxId(merchant.getTaxId())
                .status(merchant.getStatus().toString())
                .stripeAccountId(merchant.getStripeAccountId())
                .stripeVerificationStatus(merchant.getStripeVerificationStatus())
                .createdAt(merchant.getCreatedAt())
                .updatedAt(merchant.getUpdatedAt())
                .build();
    }
}
