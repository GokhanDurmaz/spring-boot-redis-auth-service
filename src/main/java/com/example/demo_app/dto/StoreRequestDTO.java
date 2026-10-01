package com.example.demo_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreRequestDTO {

    @NotBlank(message = "Store name is required")
    private String storeName;

    private String storeDescription;

    @Email(message = "Store email should be valid")
    private String storeEmail;

    private String storePhone;

    private String logoUrl;

    private String websiteUrl;

    private String address;

    private String city;

    private String state;

    private String zip;

    private String country;

    private String metadata;
}
