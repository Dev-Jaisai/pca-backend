// PlayerResponseDTO.java

package com.pca.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
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
    private Integer billingDay;
    private Integer paymentCycleMonths;
    private Boolean isActive;

    // 🔥 NEW FIELD
    private Double creditBalance;
}