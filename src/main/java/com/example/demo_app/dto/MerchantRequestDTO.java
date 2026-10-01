package com.example.demo_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantRequestDTO {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Business email is required")
    @Email(message = "Business email should be valid")
    private String businessEmail;

    private String businessPhone;

    private String businessAddress;

    private String businessCity;

    private String businessState;

    private String businessZip;

    private String businessCountry;

    private String businessDescription;

    @Pattern(regexp = "^[0-9]{2}-[0-9]{7}$|^[0-9]{9}$", message = "Tax ID format is invalid")
    private String taxId;
}
