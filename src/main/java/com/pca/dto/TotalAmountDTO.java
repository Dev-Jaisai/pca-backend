package com.pca.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TotalAmountDTO {
    private Long playerId;
    private String playerName;
    private BigDecimal totalAmount;      // Sum of all installment amounts
    private BigDecimal totalPaid;        // Sum of all paid amounts
    private BigDecimal totalRemaining;   // Sum of all remaining amounts
    private int installmentCount;        // Number of installments
    private String periodRange;          // e.g., "Jul 2025 - Dec 2025"
}