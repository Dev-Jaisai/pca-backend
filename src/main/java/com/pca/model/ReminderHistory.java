package com.pca.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminder_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // optional relation to installment for easier joins
    @ManyToOne
    @JoinColumn(name = "installment_id")
    private Installment installment;

    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    private ReminderType type; // UPCOMING, ON_DUE, OVERDUE

    private String triggeredBy; // SYSTEM or COACH

    private LocalDate snoozeDate;

    public enum ReminderType {
        UPCOMING, ON_DUE, OVERDUE
    }
}
