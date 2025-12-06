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

    private Integer periodMonth; // 1..12
    private Integer periodYear;

    @Column(nullable = false)
    private Double amount;

    private Double paidAmount;
    private Double remainingAmount;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDate dueDate;

    public enum Status {
        PENDING, PARTIALLY_PAID, PAID, OVERDUE
    }
}
