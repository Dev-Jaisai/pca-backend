package com.pca.controller;

import com.pca.dto.OverduePaymentDTO;
import com.pca.dto.PaymentRequestDTO;
import com.pca.dto.PaymentResponseDTO;
import com.pca.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public PaymentResponseDTO recordPayment(@Valid @RequestBody PaymentRequestDTO request) {
        log.info("Recording payment for installment {}", request.getInstallmentId());
        return paymentService.recordPayment(request);
    }

    @GetMapping("/installment/{installmentId}")
    public List<PaymentResponseDTO> getPayments(@PathVariable Long installmentId) {
        return paymentService.getPaymentsByInstallment(installmentId);
    }
    // inside PaymentController.java

    @PostMapping("/pay-overdue")
    public ResponseEntity<String> payOverdue(@RequestBody OverduePaymentDTO request) {
        paymentService.payOverdue(request.getPlayerId(), request.getAmount(), request.getPaymentMethod());
        return ResponseEntity.ok("Overdue payment recorded successfully");
    }
    // ✅ NEW ENDPOINT: Pay all unpaid installments
    @PostMapping("/pay-unpaid")
    public ResponseEntity<String> payUnpaid(@RequestBody OverduePaymentDTO request) {
        paymentService.payUnpaid(request.getPlayerId(), request.getAmount(), request.getPaymentMethod());
        return ResponseEntity.ok("Payment recorded successfully");
    }
}
