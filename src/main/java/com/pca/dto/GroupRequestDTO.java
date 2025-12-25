package com.pca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequestDTO {
    @NotBlank(message = "Group name is required")
    private String name;

    @NotNull(message = "Monthly fee is required")
    private Double monthlyFee;
}
