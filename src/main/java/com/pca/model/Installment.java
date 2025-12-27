package com.pca.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "installment", indexes = {
        @Index(columnList = "player_id, period_month, period_year")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    private Integer periodMonth;
    private Integer periodYear;

    @Column(nullable = false)
    private Double amount;

    private Double paidAmount;
    private Double remainingAmount;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDate dueDate;

    // 🔥 NEW FIELD (For Reasons like "Going to Village")
    @Column(length = 500)
    private String notes;

    public enum Status {
        PENDING,
        PARTIALLY_PAID,
        PAID,
        OVERDUE,
        SKIPPED,   // ✅ NEW: Holiday sathi
        REFUNDED,   // 🔥 Make sure this is added
        CANCELLED   // 🔥 Make sure this is added
    }
}