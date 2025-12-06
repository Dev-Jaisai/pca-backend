package com.pca.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentRequestDTO {
    @NotNull(message = "playerId is required")
    private Long playerId;

    @Min(value = 1) @Max(value = 12)
    private Integer periodMonth;

    @Min(value = 2000)
    private Integer periodYear;

    @NotNull(message = "dueDate is required")
    private LocalDate dueDate;

    private Double amount; // optional override
}

