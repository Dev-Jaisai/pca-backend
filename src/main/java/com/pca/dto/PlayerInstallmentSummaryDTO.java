package com.pca.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
public class PlayerInstallmentSummaryDTO {
    private Long playerId;
    private String playerName;
    private String phone;
    private String groupName;
    private LocalDate joinDate;

    private Double installmentAmount; // Use Double to match entity
    private Double totalPaid;
    private Double remaining;

    private LocalDate dueDate;
    private String status;
    private Long installmentId;
    private LocalDateTime lastPaymentDate;
    private String notes; // 🔥 Added notes

    // 🔥🔥🔥 CRITICAL: Explicit Constructor Matching Query Order 🔥🔥🔥
    // Query madhye 13 fields ahet, tithe yaach kramane (Order) constructor pahije.
    public PlayerInstallmentSummaryDTO(
            Long playerId,
            String playerName,
            String phone,
            String groupName,
            LocalDate joinDate,
            Double installmentAmount,
            Double totalPaid,
            Double remaining,
            LocalDate dueDate,
            String status,
            Long installmentId,
            LocalDateTime lastPaymentDate,
            String notes // <--- 13th Field
    ) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.phone = phone;
        this.groupName = groupName;
        this.joinDate = joinDate;
        this.installmentAmount = installmentAmount;
        this.totalPaid = totalPaid;
        this.remaining = remaining;
        this.dueDate = dueDate;
        this.status = status;
        this.installmentId = installmentId;
        this.lastPaymentDate = lastPaymentDate;
        this.notes = notes;
    }
}