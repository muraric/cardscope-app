package com.shomuran.cardscope.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "credit_card",
        indexes = {
                @Index(name = "idx_card_name", columnList = "cardProduct"),
                @Index(name = "idx_card_issuer", columnList = "issuer")
        }
)
@Getter
@Setter
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Column(nullable = false)
    private String issuer; // 🟩 NEW: e.g. "Chase", "American Express"

 //   @Column(nullable = false)
    private String cardProduct; // 🟩 e.g. "Freedom Flex", "Platinum"

    @Column(length = 10000)
    private String rewardDetails; // JSON details if needed

}
