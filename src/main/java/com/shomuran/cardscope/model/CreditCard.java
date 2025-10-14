package com.shomuran.cardscope.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "credit_card",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"issuer", "card_product"})
        },
        indexes = {
                @Index(name = "idx_card_name", columnList = "card_product"),
                @Index(name = "idx_card_issuer", columnList = "issuer")
        }
)
@Getter
@Setter
public class CreditCard extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 🏦 The bank or issuer name.
     * Example: "Chase", "American Express", "Capital One"
     */
    @Column(nullable = false)
    private String issuer;

    /**
     * 💳 The specific card product.
     * Example: "Freedom Flex", "Platinum", "Venture"
     */
    @Column(name = "card_product", nullable = false)
    private String cardProduct;

    /**
     * 🪙 Reward details (JSON string).
     * Example: {"category":"groceries","rewardRate":"5%"}
     */
    @Column(length = 10000)
    private String rewardDetails;
}
