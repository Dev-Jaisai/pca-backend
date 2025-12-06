package com.pca.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    private Long id;
    private Long installmentId;
    private Double amount;
    private LocalDateTime paidOn;
    private String paymentMethod;
    private String reference;
}
