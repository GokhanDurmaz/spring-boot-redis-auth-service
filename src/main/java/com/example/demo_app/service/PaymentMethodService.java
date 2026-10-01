package com.example.demo_app.service;

import com.example.demo_app.dto.PaymentMethodRequestDTO;
import com.example.demo_app.entity.PaymentMethod;
import com.example.demo_app.exception.PaymentException;
import com.example.demo_app.exception.PaymentMethodNotFoundException;
import com.example.demo_app.repository.PaymentMethodRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final StripeService stripeService;

    @Transactional
    public PaymentMethod addPaymentMethod(Long userId, PaymentMethodRequestDTO request) {
        log.info("Adding payment method for user: {}", userId);

        try {
            // Create payment method with Stripe
            com.stripe.model.PaymentMethod stripePaymentMethod = 
                    stripeService.createPaymentMethod(request.getStripeToken(), request.getType().toString());

            // Handle default payment method
            if (request.getIsDefault() != null && request.getIsDefault()) {
                // Mark other default payment methods as non-default
                List<PaymentMethod> defaultMethods = paymentMethodRepository
                        .findByUserIdAndIsActive(userId, true);
                defaultMethods.stream()
                        .filter(PaymentMethod::getIsDefault)
                        .forEach(m -> {
                            m.setIsDefault(false);
                            paymentMethodRepository.save(m);
                        });
            }

            // Create local payment method record
            PaymentMethod paymentMethod = PaymentMethod.builder()
                    .userId(userId)
                    .stripePaymentMethodId(stripePaymentMethod.getId())
                    .type(request.getType())
                    .cardBrand(request.getCardBrand())
                    .cardLast4(request.getCardLast4())
                    .cardExpiryMonth(request.getCardExpiryMonth())
                    .cardExpiryYear(request.getCardExpiryYear())
                    .bankAccountLast4(request.getBankAccountLast4())
                    .bankAccountHolderName(request.getBankAccountHolderName())
                    .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                    .isActive(true)
                    .build();

            paymentMethod = paymentMethodRepository.save(paymentMethod);
            log.info("Payment method added successfully: {}", paymentMethod.getId());
            return paymentMethod;

        } catch (StripeException e) {
            log.error("Failed to add payment method: {}", e.getMessage());
            throw new PaymentException("Failed to add payment method: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PaymentMethod getPaymentMethod(Long paymentMethodId, Long userId) {
        log.debug("Fetching payment method: {}", paymentMethodId);
        
        PaymentMethod method = paymentMethodRepository.findByIdAndUserId(paymentMethodId, userId)
                .orElseThrow(() -> new PaymentMethodNotFoundException(
                        "Payment method not found with id: " + paymentMethodId));
        
        return method;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> listPaymentMethods(Long userId) {
        log.debug("Listing payment methods for user: {}", userId);
        return paymentMethodRepository.findByUserIdAndIsActive(userId, true);
    }

    @Transactional
    public void deletePaymentMethod(Long paymentMethodId, Long userId) {
        log.info("Deleting payment method: {}", paymentMethodId);

        PaymentMethod method = getPaymentMethod(paymentMethodId, userId);

        try {
            // Detach from Stripe
            stripeService.detachPaymentMethod(method.getStripePaymentMethodId());

            // Deactivate in local database instead of deleting
            method.setIsActive(false);
            paymentMethodRepository.save(method);
            log.info("Payment method deleted successfully: {}", paymentMethodId);

        } catch (StripeException e) {
            log.error("Failed to delete payment method: {}", e.getMessage());
            throw new PaymentException("Failed to delete payment method: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void setDefaultPaymentMethod(Long paymentMethodId, Long userId) {
        log.info("Setting default payment method: {}", paymentMethodId);

        PaymentMethod method = getPaymentMethod(paymentMethodId, userId);

        // Mark all other methods as non-default
        List<PaymentMethod> userMethods = paymentMethodRepository.findByUserIdAndIsActive(userId, true);
        userMethods.forEach(m -> {
            m.setIsDefault(false);
            paymentMethodRepository.save(m);
        });

        // Set this method as default
        method.setIsDefault(true);
        paymentMethodRepository.save(method);
        log.info("Default payment method updated: {}", paymentMethodId);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentMethod> getDefaultPaymentMethod(Long userId) {
        log.debug("Fetching default payment method for user: {}", userId);
        
        List<PaymentMethod> methods = paymentMethodRepository.findByUserIdAndIsActive(userId, true);
        return methods.stream()
                .filter(PaymentMethod::getIsDefault)
                .findFirst();
    }
}
