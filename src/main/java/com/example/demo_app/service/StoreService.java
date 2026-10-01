package com.example.demo_app.service;

import com.example.demo_app.dto.StoreRequestDTO;
import com.example.demo_app.entity.Merchant;
import com.example.demo_app.entity.Store;
import com.example.demo_app.exception.MerchantNotFoundException;
import com.example.demo_app.exception.PaymentException;
import com.example.demo_app.exception.StoreNotFoundException;
import com.example.demo_app.repository.MerchantRepository;
import com.example.demo_app.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final MerchantRepository merchantRepository;

    @Transactional
    public Store createStore(Long merchantId, Long userId, StoreRequestDTO request) {
        log.info("Creating store for merchant: {}", merchantId);

        // Verify merchant exists
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException("Merchant not found with id: " + merchantId));

        // Verify ownership
        if (!merchant.getUserId().equals(userId)) {
            throw new PaymentException("Unauthorized: You can only create stores for your own merchant account");
        }

        // Create store
        Store store = Store.builder()
                .merchant(merchant)
                .storeName(request.getStoreName())
                .storeDescription(request.getStoreDescription())
                .storeEmail(request.getStoreEmail())
                .storePhone(request.getStorePhone())
                .logoUrl(request.getLogoUrl())
                .websiteUrl(request.getWebsiteUrl())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .zip(request.getZip())
                .country(request.getCountry())
                .metadata(request.getMetadata())
                .status(Store.StoreStatus.ACTIVE)
                .build();

        store = storeRepository.save(store);
        log.info("Store created successfully: {}", store.getId());
        return store;
    }

    @Transactional(readOnly = true)
    public Store getStore(Long storeId) {
        log.debug("Fetching store: {}", storeId);
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException("Store not found with id: " + storeId));
    }

    @Transactional(readOnly = true)
    public Store getStoreByIdAndMerchantId(Long storeId, Long merchantId) {
        log.debug("Fetching store: {} for merchant: {}", storeId, merchantId);
        return storeRepository.findByIdAndMerchantId(storeId, merchantId)
                .orElseThrow(() -> new StoreNotFoundException("Store not found with id: " + storeId + " for merchant: " + merchantId));
    }

    @Transactional
    public Store updateStore(Long storeId, Long merchantId, Long userId, StoreRequestDTO request) {
        log.info("Updating store: {}", storeId);

        Store store = getStoreByIdAndMerchantId(storeId, merchantId);

        // Verify ownership
        if (!store.getMerchant().getUserId().equals(userId)) {
            throw new PaymentException("Unauthorized: You can only update your own stores");
        }

        store.setStoreName(request.getStoreName());
        store.setStoreDescription(request.getStoreDescription());
        store.setStoreEmail(request.getStoreEmail());
        store.setStorePhone(request.getStorePhone());
        store.setLogoUrl(request.getLogoUrl());
        store.setWebsiteUrl(request.getWebsiteUrl());
        store.setAddress(request.getAddress());
        store.setCity(request.getCity());
        store.setState(request.getState());
        store.setZip(request.getZip());
        store.setCountry(request.getCountry());
        store.setMetadata(request.getMetadata());

        store = storeRepository.save(store);
        log.info("Store updated successfully: {}", storeId);
        return store;
    }

    @Transactional
    public void deleteStore(Long storeId, Long merchantId, Long userId) {
        log.info("Deleting store: {}", storeId);

        Store store = getStoreByIdAndMerchantId(storeId, merchantId);

        // Verify ownership
        if (!store.getMerchant().getUserId().equals(userId)) {
            throw new PaymentException("Unauthorized: You can only delete your own stores");
        }

        storeRepository.delete(store);
        log.info("Store deleted successfully: {}", storeId);
    }

    @Transactional(readOnly = true)
    public List<Store> listStoresByMerchant(Long merchantId) {
        log.debug("Listing stores for merchant: {}", merchantId);

        // Verify merchant exists
        if (!merchantRepository.existsById(merchantId)) {
            throw new MerchantNotFoundException("Merchant not found with id: " + merchantId);
        }

        return storeRepository.findByMerchantId(merchantId);
    }

    @Transactional(readOnly = true)
    public List<Store> listStoresByStatus(Store.StoreStatus status) {
        log.debug("Listing stores with status: {}", status);
        return storeRepository.findByStatus(status);
    }

    @Transactional
    public void updateStoreStatus(Long storeId, Store.StoreStatus newStatus) {
        log.info("Updating store status: {} -> {}", storeId, newStatus);
        Store store = getStore(storeId);
        store.setStatus(newStatus);
        storeRepository.save(store);
    }
}
