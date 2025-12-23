package com.pca.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerResponseDTO {
    private Long id;
    private String name;
    private String phone;
    private Integer age;
    private LocalDate joinDate;

    private Long groupId;
    private String groupName;

    private String notes;
    private String photoUrl;

    // ✅ हे दोन नवीन फील्ड्स ॲड करा
    private Integer billingDay;
    private Integer paymentCycleMonths;
}