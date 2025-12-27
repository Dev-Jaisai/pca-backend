package com.pca.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "player")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String phone;
    private Integer age;
    private LocalDate joinDate;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private GroupEntity playerGroup;

    @Column(length = 1024)
    private String notes;

    private String photoUrl;

    @Column(name = "billing_day")
    private Integer billingDay;

    @Column(name = "payment_cycle_months")
    @Builder.Default
    private Integer paymentCycleMonths = 1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // 🔥🔥🔥 NEW FIELD FOR ADVANCE PAYMENT 🔥🔥🔥
    @Column(name = "credit_balance")
    @Builder.Default
    private Double creditBalance = 0.0;
}