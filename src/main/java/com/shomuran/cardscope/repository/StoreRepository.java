package com.shomuran.cardscope.repository;

import com.shomuran.cardscope.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    /**
     * Find store by name (case-insensitive).
     */
    Optional<Store> findByStoreNameIgnoreCase(String storeName);

    /**
     * Check if store exists by name (case-insensitive).
     */
    boolean existsByStoreNameIgnoreCase(String storeName);
}
