package com.pca.dto;

import lombok.Data;

@Data
public class OverduePaymentDTO {
    private Long playerId;
    private Double amount;
    private String paymentMethod; // e.g., "Cash", "UPI"
}