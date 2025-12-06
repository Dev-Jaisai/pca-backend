package com.pca.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "fee_structure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeStructure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupEntity group;

    @Column(nullable = false)
    private Double monthlyFee;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
