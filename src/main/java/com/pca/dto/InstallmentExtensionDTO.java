package com.pca.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentExtensionDTO {
    @NotNull(message = "installmentId is required")
    private Long installmentId;

    @NotNull(message = "newDueDate is required")
    private LocalDate newDueDate;
}