package com.example.demo_app.controller;

import com.example.demo_app.dto.PaymentRequestDTO;
import com.example.demo_app.dto.PaymentResponseDTO;
import com.example.demo_app.entity.Payment;
import com.example.demo_app.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @Valid @RequestBody PaymentRequestDTO request,
            Authentication authentication) {
        log.info("Create payment request");

        Long customerId = extractUserIdFromAuthentication(authentication);
        Payment payment = paymentService.createPayment(customerId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentResponseDTO.fromEntity(payment));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getPayment(
            @PathVariable Long paymentId) {
        log.debug("Get payment request: {}", paymentId);

        Payment payment = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(PaymentResponseDTO.fromEntity(payment));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<PaymentResponseDTO>> listCustomerPayments(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy) {
        log.debug("List payments for customer: {}", customerId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> payments = paymentService.listCustomerPayments(customerId, pageable, status);
        
        return ResponseEntity.ok(payments.map(PaymentResponseDTO::fromEntity));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<Page<PaymentResponseDTO>> listStorePayments(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId) {
        log.debug("List payments for store: {}", storeId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> payments = paymentService.listStorePayments(storeId, pageable, status);
        
        return ResponseEntity.ok(payments.map(PaymentResponseDTO::fromEntity));
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
