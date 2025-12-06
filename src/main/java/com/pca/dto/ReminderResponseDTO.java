package com.pca.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderResponseDTO {
    private Long id;
    private Long installmentId;
    private LocalDateTime sentAt;
    private String type;
    private String triggeredBy;
    private LocalDate snoozeDate;
}
