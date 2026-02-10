package com.shomuran.cardscope.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "store",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"store_name"})
        },
        indexes = {
                @Index(name = "idx_store_name", columnList = "store_name"),
                @Index(name = "idx_store_category", columnList = "category")
        }
)
@Getter
@Setter
public class Store extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 🏪 Store name (case-insensitive lookup).
     * Example: "Walmart", "Target", "Amazon.com"
     */
    @Column(name = "store_name", nullable = false, unique = true)
    private String storeName;

    /**
     * 📂 Category for this store.
     * Example: "groceries", "department_store", "gas", "dining", "online_retail"
     */
    @Column(nullable = false)
    private String category;

    /**
     * 📝 Optional notes about the store or category mapping.
     */
    @Column(length = 500)
    private String notes;
}
