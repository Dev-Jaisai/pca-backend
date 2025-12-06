package com.pca.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructureRequestDTO {
    @NotNull(message = "groupId is required")
    private Long groupId;

    @NotNull(message = "monthlyFee is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private Double monthlyFee;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
