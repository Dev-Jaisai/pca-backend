package com.pca.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {
    @NotNull(message = "installmentId is required")
    private Long installmentId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01")
    private Double amount;

    private String paymentMethod;
    private String reference;
}
