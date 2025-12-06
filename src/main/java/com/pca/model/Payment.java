package com.pca.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "installment_id", nullable = false)
    private Installment installment;

    @Column(nullable = false)
    private Double amount;

    private LocalDateTime paidOn;

    private String paymentMethod; // Cash, UPI, Card, BankTransfer etc.
    private String reference; // tx id / note
}
