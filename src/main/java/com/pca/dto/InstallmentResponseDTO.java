package com.pca.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentResponseDTO {
    private Long installmentId;
    private Long playerId;
    private String playerName;
    private Integer periodMonth;
    private Integer periodYear;
    private Double amount;
    private Double paidAmount;
    private Double remainingAmount;
    private String status;
    private LocalDate dueDate;

    // 🔥🔥 ADD THIS FIELD 🔥🔥
    private String notes;
}