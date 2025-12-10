package com.pca.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerInstallmentSummaryDTO {
    private Long playerId;
    private String playerName;
    private String phone;
    private String groupName;
    private LocalDate joinDate;

    private BigDecimal installmentAmount;
    private BigDecimal totalPaid;
    private BigDecimal remaining;

    private LocalDate dueDate;
    private String status; // Paid / Partially Paid / Pending / Overdue
    private Long installmentId;
    private LocalDateTime lastPaymentDate; // Add this field
}
