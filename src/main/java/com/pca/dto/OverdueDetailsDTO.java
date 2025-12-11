package com.pca.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverdueDetailsDTO {
    private Long playerId;
    private String playerName;
    private int overdueMonthCount;
    private BigDecimal totalOverdueAmount;
    private BigDecimal totalPaid;
    private BigDecimal totalRemaining;
    private List<OverdueMonthDTO> overdueMonths;
    private YearMonth firstOverdueMonth;
    private YearMonth latestOverdueMonth;
}