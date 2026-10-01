package com.example.demo_app.controller;

import com.example.demo_app.dto.MerchantRequestDTO;
import com.example.demo_app.dto.MerchantResponseDTO;
import com.example.demo_app.entity.Merchant;
import com.example.demo_app.service.MerchantService;
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
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping
    public ResponseEntity<MerchantResponseDTO> registerMerchant(
            @Valid @RequestBody MerchantRequestDTO request,
            Authentication authentication) {
        log.info("Register merchant request for user: {}", authentication.getName());
        
        // Get the current user ID from security context
        Long userId = extractUserIdFromAuthentication(authentication);
        
        Merchant merchant = merchantService.registerMerchant(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MerchantResponseDTO.fromEntity(merchant));
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantResponseDTO> getMerchant(
            @PathVariable Long merchantId,
            Authentication authentication) {
        log.debug("Get merchant request: {}", merchantId);
        
        Merchant merchant = merchantService.getMerchantById(merchantId);
        Long userId = extractUserIdFromAuthentication(authentication);
        
        // Verify ownership (optional - depends on requirements)
        // For now, allow viewing any merchant
        
        return ResponseEntity.ok(MerchantResponseDTO.fromEntity(merchant));
    }

    @PutMapping("/{merchantId}")
    public ResponseEntity<MerchantResponseDTO> updateMerchant(
            @PathVariable Long merchantId,
            @Valid @RequestBody MerchantRequestDTO request,
            Authentication authentication) {
        log.info("Update merchant request: {}", merchantId);
        
        Long userId = extractUserIdFromAuthentication(authentication);
        Merchant merchant = merchantService.updateMerchantProfile(merchantId, userId, request);
        
        return ResponseEntity.ok(MerchantResponseDTO.fromEntity(merchant));
    }

    @GetMapping
    public ResponseEntity<List<MerchantResponseDTO>> listMerchants() {
        log.debug("List merchants request");
        
        List<Merchant> merchants = merchantService.listMerchants();
        List<MerchantResponseDTO> response = merchants.stream()
                .map(MerchantResponseDTO::fromEntity)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MerchantResponseDTO> getCurrentMerchant(Authentication authentication) {
        log.debug("Get current merchant for user: {}", authentication.getName());
        
        Long userId = extractUserIdFromAuthentication(authentication);
        Merchant merchant = merchantService.getMerchantByUserId(userId);
        
        return ResponseEntity.ok(MerchantResponseDTO.fromEntity(merchant));
    }

    /**
     * Extract user ID from authentication object
     * This assumes the user principal has an id property or similar
     */
    private Long extractUserIdFromAuthentication(Authentication authentication) {
        // This is a simplified version - in production, you might have a UserPrincipal or similar
        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                // In this case, we use the username as userId (simplified)
                // In production, you should have a proper user service to look up the user ID
                return 1L; // Placeholder - should get from security context or user service
            }
        } catch (Exception e) {
            log.warn("Failed to extract user ID from authentication", e);
        }
        return 1L; // Placeholder
    }
}
