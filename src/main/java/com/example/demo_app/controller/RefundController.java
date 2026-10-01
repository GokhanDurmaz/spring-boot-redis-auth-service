package com.example.demo_app.controller;

import com.example.demo_app.dto.RefundRequestDTO;
import com.example.demo_app.dto.RefundResponseDTO;
import com.example.demo_app.entity.Refund;
import com.example.demo_app.service.RefundService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/payments/{paymentId}/refunds")
    public ResponseEntity<RefundResponseDTO> refundPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundRequestDTO request,
            Authentication authentication) {
        log.info("Refund payment request: {}", paymentId);

        Long merchantId = extractMerchantIdFromAuthentication(authentication);
        Refund refund = refundService.refundPayment(paymentId, merchantId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RefundResponseDTO.fromEntity(refund));
    }

    @PostMapping("/payments/{paymentId}/cancel")
    public ResponseEntity<Void> cancelPayment(
            @PathVariable Long paymentId,
            Authentication authentication) {
        log.info("Cancel payment request: {}", paymentId);

        Long merchantId = extractMerchantIdFromAuthentication(authentication);
        refundService.cancelPayment(paymentId, merchantId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<RefundResponseDTO> getRefund(
            @PathVariable Long refundId) {
        log.debug("Get refund request: {}", refundId);

        Refund refund = refundService.getRefund(refundId);
        return ResponseEntity.ok(RefundResponseDTO.fromEntity(refund));
    }

    @GetMapping("/payments/{paymentId}/refunds")
    public ResponseEntity<List<RefundResponseDTO>> getPaymentRefunds(
            @PathVariable Long paymentId) {
        log.debug("Get payment refunds request: {}", paymentId);

        List<Refund> refunds = refundService.getPaymentRefunds(paymentId);
        List<RefundResponseDTO> response = refunds.stream()
                .map(RefundResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Extract merchant ID from authentication object
     */
    private Long extractMerchantIdFromAuthentication(Authentication authentication) {
        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                return 1L; // Placeholder - should get from security context or user service
            }
        } catch (Exception e) {
            log.warn("Failed to extract merchant ID from authentication", e);
        }
        return 1L; // Placeholder
    }
}
