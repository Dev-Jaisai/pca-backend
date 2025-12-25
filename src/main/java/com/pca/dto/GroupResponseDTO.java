package com.pca.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponseDTO {
    private Long id;//groupId
    private String name;
    private Double currentFee; // हे नवीन फील्ड, लिस्टमध्ये फी दाखवण्यासाठी
}
