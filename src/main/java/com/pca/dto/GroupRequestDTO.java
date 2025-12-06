package com.pca.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequestDTO {
    @NotBlank(message = "Group name is required")
    private String name;
}
