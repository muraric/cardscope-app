package com.shomuran.cardscope.repository;

import com.shomuran.cardscope.model.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    /**
     * 🔹 Find distinct issuers that match a given search term (case-insensitive).
     * Example: search='ame' → returns ["American Express", "American Credit Union"]
     */
    @Query("SELECT DISTINCT c.issuer FROM CreditCard c " +
            "WHERE LOWER(c.issuer) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<String> findDistinctIssuerByNameContainingIgnoreCase(String search);

    /**
     * 🔹 Find distinct card products for a specific issuer.
     * Example: issuer='Chase', search='free' → ["Freedom Flex", "Freedom Unlimited"]
     */
    @Query("SELECT DISTINCT c.cardProduct FROM CreditCard c " +
            "WHERE LOWER(c.issuer) = LOWER(:issuer) " +
            "AND LOWER(c.cardProduct) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<String> findProductsByIssuer(String issuer, String search);

    /**
     * 🔹 Find a specific card by issuer and product.
     */
    Optional<CreditCard> findByIssuerIgnoreCaseAndCardProductIgnoreCase(String issuer, String cardProduct);

    boolean existsByIssuerIgnoreCaseAndCardProductIgnoreCase(String issuer, String cardProduct);

    /**
     * ✅ Find all cards whose rewardDetails are empty ("{}") or NULL.
     * Uses TRIM + REPLACE to ignore whitespace around braces.
     * Example: matches "{}", " { } ", or null.
     */
    @Query("""
           SELECT c
           FROM CreditCard c
           WHERE c.rewardDetails IS NULL
              OR REPLACE(TRIM(c.rewardDetails), ' ', '') = '{}'
           """)
    List<CreditCard> findCardsWithEmptyRewards();
}
