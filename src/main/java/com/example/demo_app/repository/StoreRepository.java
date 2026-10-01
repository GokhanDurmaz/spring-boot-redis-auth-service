package com.example.demo_app.repository;

import com.example.demo_app.entity.Store;
import com.example.demo_app.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByMerchant(Merchant merchant);
    List<Store> findByMerchantId(Long merchantId);
    Optional<Store> findByIdAndMerchantId(Long storeId, Long merchantId);
    List<Store> findByStatus(Store.StoreStatus status);
}
