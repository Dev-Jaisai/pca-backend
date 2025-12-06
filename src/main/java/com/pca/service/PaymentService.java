package com.pca.service;

import com.pca.dto.PaymentRequestDTO;
import com.pca.dto.PaymentResponseDTO;
import com.pca.exception.ResourceNotFoundException;
import com.pca.model.Installment;
import com.pca.model.Payment;
import com.pca.repository.PaymentRepository;
import com.pca.repository.InstallmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InstallmentRepository installmentRepository;

    @Transactional
    public PaymentResponseDTO recordPayment(PaymentRequestDTO req) {
        log.info("Recording payment for installment {} amount {}", req.getInstallmentId(), req.getAmount());
        Installment inst = installmentRepository.findById(req.getInstallmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found: " + req.getInstallmentId()));

        Payment p = Payment.builder()
                .installment(inst)
                .amount(req.getAmount())
                .paidOn(LocalDateTime.now())
                .paymentMethod(req.getPaymentMethod())
                .reference(req.getReference())
                .build();

        Payment saved = paymentRepository.save(p);

        // update installment amounts and status
        double prevPaid = inst.getPaidAmount() == null ? 0.0 : inst.getPaidAmount();
        double newPaid = prevPaid + saved.getAmount();
        inst.setPaidAmount(newPaid);
        double remaining = inst.getAmount() - newPaid;
        inst.setRemainingAmount(Math.max(0.0, remaining));

        if (remaining <= 0.0) {
            inst.setStatus(Installment.Status.PAID);
        } else if (newPaid > 0.0) {
            inst.setStatus(Installment.Status.PARTIALLY_PAID);
        } else {
            inst.setStatus(Installment.Status.PENDING);
        }

        installmentRepository.save(inst);
        log.info("Updated installment {} paidAmount={} remaining={} status={}", inst.getId(), inst.getPaidAmount(), inst.getRemainingAmount(), inst.getStatus());

        return toDto(saved);
    }

    public List<PaymentResponseDTO> getPaymentsByInstallment(Long installmentId) {
        return paymentRepository.findByInstallmentIdOrderByPaidOnDesc(installmentId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private PaymentResponseDTO toDto(Payment p) {
        return PaymentResponseDTO.builder()
                .id(p.getId())
                .installmentId(p.getInstallment().getId())
                .amount(p.getAmount())
                .paidOn(p.getPaidOn())
                .paymentMethod(p.getPaymentMethod())
                .reference(p.getReference())
                .build();
    }
}
