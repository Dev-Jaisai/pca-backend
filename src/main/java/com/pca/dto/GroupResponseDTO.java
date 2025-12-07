package com.pca.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponseDTO {
    private Long id;//groupId
    private String name;
}
