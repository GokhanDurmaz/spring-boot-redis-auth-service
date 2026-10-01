package com.example.demo_app.controller;

import com.example.demo_app.dto.StoreRequestDTO;
import com.example.demo_app.dto.StoreResponseDTO;
import com.example.demo_app.entity.Store;
import com.example.demo_app.service.StoreService;
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
public class StoreController {

    private final StoreService storeService;

    @PostMapping("/merchants/{merchantId}/stores")
    public ResponseEntity<StoreResponseDTO> createStore(
            @PathVariable Long merchantId,
            @Valid @RequestBody StoreRequestDTO request,
            Authentication authentication) {
        log.info("Create store request for merchant: {}", merchantId);

        Long userId = extractUserIdFromAuthentication(authentication);
        Store store = storeService.createStore(merchantId, userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StoreResponseDTO.fromEntity(store));
    }

    @GetMapping("/stores/{storeId}")
    public ResponseEntity<StoreResponseDTO> getStore(
            @PathVariable Long storeId) {
        log.debug("Get store request: {}", storeId);

        Store store = storeService.getStore(storeId);
        return ResponseEntity.ok(StoreResponseDTO.fromEntity(store));
    }

    @PutMapping("/merchants/{merchantId}/stores/{storeId}")
    public ResponseEntity<StoreResponseDTO> updateStore(
            @PathVariable Long merchantId,
            @PathVariable Long storeId,
            @Valid @RequestBody StoreRequestDTO request,
            Authentication authentication) {
        log.info("Update store request: {}", storeId);

        Long userId = extractUserIdFromAuthentication(authentication);
        Store store = storeService.updateStore(storeId, merchantId, userId, request);

        return ResponseEntity.ok(StoreResponseDTO.fromEntity(store));
    }

    @DeleteMapping("/merchants/{merchantId}/stores/{storeId}")
    public ResponseEntity<Void> deleteStore(
            @PathVariable Long merchantId,
            @PathVariable Long storeId,
            Authentication authentication) {
        log.info("Delete store request: {}", storeId);

        Long userId = extractUserIdFromAuthentication(authentication);
        storeService.deleteStore(storeId, merchantId, userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/merchants/{merchantId}/stores")
    public ResponseEntity<List<StoreResponseDTO>> listStoresByMerchant(
            @PathVariable Long merchantId) {
        log.debug("List stores for merchant: {}", merchantId);

        List<Store> stores = storeService.listStoresByMerchant(merchantId);
        List<StoreResponseDTO> response = stores.stream()
                .map(StoreResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Extract user ID from authentication object
     * This is a simplified version - in production, implement proper user context extraction
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
