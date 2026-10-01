package com.example.demo_app.controller;

import com.example.demo_app.dto.PaymentMethodRequestDTO;
import com.example.demo_app.dto.PaymentMethodResponseDTO;
import com.example.demo_app.entity.PaymentMethod;
import com.example.demo_app.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @PostMapping
    public ResponseEntity<PaymentMethodResponseDTO> addPaymentMethod(
            @Valid @RequestBody PaymentMethodRequestDTO request,
            Authentication authentication) {
        log.info("Add payment method request");

        Long userId = extractUserIdFromAuthentication(authentication);
        PaymentMethod paymentMethod = paymentMethodService.addPaymentMethod(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentMethodResponseDTO.fromEntity(paymentMethod));
    }

    @GetMapping("/{paymentMethodId}")
    public ResponseEntity<PaymentMethodResponseDTO> getPaymentMethod(
            @PathVariable Long paymentMethodId,
            Authentication authentication) {
        log.debug("Get payment method request: {}", paymentMethodId);

        Long userId = extractUserIdFromAuthentication(authentication);
        PaymentMethod paymentMethod = paymentMethodService.getPaymentMethod(paymentMethodId, userId);

        return ResponseEntity.ok(PaymentMethodResponseDTO.fromEntity(paymentMethod));
    }

    @GetMapping
    public ResponseEntity<List<PaymentMethodResponseDTO>> listPaymentMethods(
            Authentication authentication) {
        log.debug("List payment methods request");

        Long userId = extractUserIdFromAuthentication(authentication);
        List<PaymentMethod> methods = paymentMethodService.listPaymentMethods(userId);

        List<PaymentMethodResponseDTO> response = methods.stream()
                .map(PaymentMethodResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{paymentMethodId}")
    public ResponseEntity<Void> deletePaymentMethod(
            @PathVariable Long paymentMethodId,
            Authentication authentication) {
        log.info("Delete payment method request: {}", paymentMethodId);

        Long userId = extractUserIdFromAuthentication(authentication);
        paymentMethodService.deletePaymentMethod(paymentMethodId, userId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{paymentMethodId}/set-default")
    public ResponseEntity<PaymentMethodResponseDTO> setDefaultPaymentMethod(
            @PathVariable Long paymentMethodId,
            Authentication authentication) {
        log.info("Set default payment method request: {}", paymentMethodId);

        Long userId = extractUserIdFromAuthentication(authentication);
        paymentMethodService.setDefaultPaymentMethod(paymentMethodId, userId);
        PaymentMethod paymentMethod = paymentMethodService.getPaymentMethod(paymentMethodId, userId);

        return ResponseEntity.ok(PaymentMethodResponseDTO.fromEntity(paymentMethod));
    }

    /**
     * Extract user ID from authentication object
     */
    private Long extractUserIdFromAuthentication(Authentication authentication) {
        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                return 1L; // Placeholder - should get from security context or user service
            }
        } catch (Exception e) {
            log.warn("Failed to extract user ID from authentication", e);
        }
        return 1L; // Placeholder
    }
}
