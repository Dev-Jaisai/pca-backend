package com.pca.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructureResponseDTO {
    private Long id;
    private Long groupId;
    private String groupName;
    private Double monthlyFee;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
