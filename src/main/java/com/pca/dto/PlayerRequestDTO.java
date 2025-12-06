package com.pca.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerRequestDTO {
    @NotBlank(message = "Player name is required")
    private String name;

    private String phone;
    private Integer age;
    private LocalDate joinDate;

    @NotNull(message = "groupId is required")
    private Long groupId;

    private String notes;
    private String photoUrl;
}
