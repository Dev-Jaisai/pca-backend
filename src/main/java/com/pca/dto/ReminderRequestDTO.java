package com.pca.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderRequestDTO {

    @NotNull(message = "installmentId is required")
    private Long installmentId;

    @NotNull(message = "type is required")
    private String type; // UPCOMING, ON_DUE, OVERDUE

    private String triggeredBy; // SYSTEM / COACH
    private LocalDate snoozeDate;
}
