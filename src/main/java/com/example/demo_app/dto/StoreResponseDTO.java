package com.example.demo_app.dto;

import com.example.demo_app.entity.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponseDTO {

    private Long id;

    private Long merchantId;

    private String storeName;

    private String storeDescription;

    private String storeEmail;

    private String storePhone;

    private String logoUrl;

    private String websiteUrl;

    private String address;

    private String city;

    private String state;

    private String zip;

    private String country;

    private String status;

    private String metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static StoreResponseDTO fromEntity(Store store) {
        return StoreResponseDTO.builder()
                .id(store.getId())
                .merchantId(store.getMerchant().getId())
                .storeName(store.getStoreName())
                .storeDescription(store.getStoreDescription())
                .storeEmail(store.getStoreEmail())
                .storePhone(store.getStorePhone())
                .logoUrl(store.getLogoUrl())
                .websiteUrl(store.getWebsiteUrl())
                .address(store.getAddress())
                .city(store.getCity())
                .state(store.getState())
                .zip(store.getZip())
                .country(store.getCountry())
                .status(store.getStatus().toString())
                .metadata(store.getMetadata())
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
    }
}
