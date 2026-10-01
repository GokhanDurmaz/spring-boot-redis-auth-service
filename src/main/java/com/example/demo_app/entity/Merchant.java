package com.example.demo_app.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String businessName;

    @Column(nullable = false)
    private String businessEmail;

    @Column
    private String businessPhone;

    @Column(length = 500)
    private String businessAddress;

    @Column
    private String businessCity;

    @Column
    private String businessState;

    @Column
    private String businessZip;

    @Column
    private String businessCountry;

    @Column(length = 1000)
    private String businessDescription;

    @Column
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status;

    @Column
    private String stripeAccountId;

    @Column
    private String stripeVerificationStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MerchantStatus {
        PENDING_VERIFICATION,
        VERIFIED,
        SUSPENDED,
        ACTIVE
    }
}
