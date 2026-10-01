package com.example.demo_app.service;

import com.example.demo_app.dto.MerchantRequestDTO;
import com.example.demo_app.entity.Merchant;
import com.example.demo_app.exception.MerchantNotFoundException;
import com.example.demo_app.exception.PaymentException;
import com.example.demo_app.repository.MerchantRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final StripeService stripeService;

    @Transactional
    public Merchant registerMerchant(Long userId, MerchantRequestDTO request) {
        log.info("Registering merchant for user: {}", userId);

        // Check if merchant already exists for this user
        Optional<Merchant> existingMerchant = merchantRepository.findByUserId(userId);
        if (existingMerchant.isPresent()) {
            throw new PaymentException("Merchant already registered for this user");
        }

        // Check if business name already exists
        Optional<Merchant> existingByName = merchantRepository.findByBusinessName(request.getBusinessName());
        if (existingByName.isPresent()) {
            throw new PaymentException("Business name already exists");
        }

        try {
            // Create Stripe connected account
            Account stripeAccount = stripeService.createConnectedAccount(
                    request.getBusinessName(),
                    request.getBusinessEmail()
            );

            // Create merchant in database
            Merchant merchant = Merchant.builder()
                    .userId(userId)
                    .businessName(request.getBusinessName())
                    .businessEmail(request.getBusinessEmail())
                    .businessPhone(request.getBusinessPhone())
                    .businessAddress(request.getBusinessAddress())
                    .businessCity(request.getBusinessCity())
                    .businessState(request.getBusinessState())
                    .businessZip(request.getBusinessZip())
                    .businessCountry(request.getBusinessCountry())
                    .businessDescription(request.getBusinessDescription())
                    .taxId(request.getTaxId())
                    .stripeAccountId(stripeAccount.getId())
                    .status(Merchant.MerchantStatus.PENDING_VERIFICATION)
                    .stripeVerificationStatus(stripeAccount.getChargesEnabled() ? "VERIFIED" : "PENDING")
                    .build();

            merchant = merchantRepository.save(merchant);
            log.info("Merchant registered successfully: {}", merchant.getId());
            return merchant;

        } catch (StripeException e) {
            log.error("Failed to create Stripe account: {}", e.getMessage());
            throw new PaymentException("Failed to create Stripe account: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Merchant getMerchantById(Long merchantId) {
        log.debug("Fetching merchant: {}", merchantId);
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException("Merchant not found with id: " + merchantId));
    }

    @Transactional(readOnly = true)
    public Merchant getMerchantByUserId(Long userId) {
        log.debug("Fetching merchant for user: {}", userId);
        return merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new MerchantNotFoundException("Merchant not found for user: " + userId));
    }

    @Transactional
    public Merchant updateMerchantProfile(Long merchantId, Long userId, MerchantRequestDTO request) {
        log.info("Updating merchant profile: {}", merchantId);

        Merchant merchant = getMerchantById(merchantId);

        // Verify ownership
        if (!merchant.getUserId().equals(userId)) {
            throw new PaymentException("Unauthorized: You can only update your own merchant profile");
        }

        merchant.setBusinessName(request.getBusinessName());
        merchant.setBusinessEmail(request.getBusinessEmail());
        merchant.setBusinessPhone(request.getBusinessPhone());
        merchant.setBusinessAddress(request.getBusinessAddress());
        merchant.setBusinessCity(request.getBusinessCity());
        merchant.setBusinessState(request.getBusinessState());
        merchant.setBusinessZip(request.getBusinessZip());
        merchant.setBusinessCountry(request.getBusinessCountry());
        merchant.setBusinessDescription(request.getBusinessDescription());
        merchant.setTaxId(request.getTaxId());

        merchant = merchantRepository.save(merchant);
        log.info("Merchant profile updated successfully: {}", merchantId);
        return merchant;
    }

    @Transactional(readOnly = true)
    public List<Merchant> listMerchants() {
        log.debug("Listing all merchants");
        return merchantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Merchant> listMerchantsByStatus(Merchant.MerchantStatus status) {
        log.debug("Listing merchants with status: {}", status);
        return merchantRepository.findByStatus(status);
    }

    @Transactional
    public void updateMerchantVerificationStatus(Long merchantId, String verificationStatus) {
        log.info("Updating merchant verification status: {} -> {}", merchantId, verificationStatus);
        Merchant merchant = getMerchantById(merchantId);
        merchant.setStripeVerificationStatus(verificationStatus);
        if ("VERIFIED".equals(verificationStatus)) {
            merchant.setStatus(Merchant.MerchantStatus.ACTIVE);
        }
        merchantRepository.save(merchant);
    }
}
