// com.pca.dto.InstallmentStatusDTO.java
package com.pca.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentStatusDTO {
    private Long playerId;
    private boolean hasInstallments;
}
