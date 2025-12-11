package com.pca.service;

import com.pca.dto.PaymentRequestDTO;
import com.pca.dto.PaymentResponseDTO;
import com.pca.exception.BadRequestException;
import com.pca.exception.ResourceNotFoundException;
import com.pca.model.Installment;
import com.pca.model.Installment.Status;
import com.pca.model.Payment;
import com.pca.repository.InstallmentRepository;
import com.pca.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InstallmentRepository installmentRepository;

    /**
     * Record a payment against an installment.
     * Validations:
     *  - installment must exist
     *  - amount must be > 0
     *  - cannot pay if installment is already fully paid
     *  - disallow overpayment (business decision)
     */
    @Transactional
    public PaymentResponseDTO recordPayment(PaymentRequestDTO req) {
        log.info("Recording payment for installment {} amount {}", req.getInstallmentId(), req.getAmount());

        Installment inst = installmentRepository.findById(req.getInstallmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found: " + req.getInstallmentId()));

        double remaining = (inst.getRemainingAmount() == null) ? inst.getAmount() : inst.getRemainingAmount();

        // Prevent paying when already paid
        if (remaining <= 0.0) {
            log.warn("Attempt to pay an already paid installment: {}", inst.getId());
            throw new BadRequestException("Installment is already fully paid.");
        }

        // Validate amount
        if (req.getAmount() == null || req.getAmount() <= 0.0) {
            throw new BadRequestException("Payment amount must be greater than 0.");
        }

        // Prevent overpayment (remove this check if you want to allow overpayment)
        if (req.getAmount() > remaining) {
            throw new BadRequestException(String.format("Payment amount (%.2f) exceeds remaining amount (%.2f).", req.getAmount(), remaining));
        }

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
        double newRemaining = inst.getAmount() - newPaid;
        inst.setRemainingAmount(Math.max(0.0, newRemaining));

        if (newRemaining <= 0.0) {
            inst.setStatus(Status.PAID);
        } else if (newPaid > 0.0) {
            inst.setStatus(Status.PARTIALLY_PAID);
        } else {
            inst.setStatus(Status.PENDING);
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
    // inside PaymentService class

    @Transactional
    public void payOverdue(Long playerId, Double totalPaymentAmount, String method) {
        // 1. Get overdue installments sorted by date (oldest first)
        List<Installment> overdueList = installmentRepository.findOverdueInstallmentsByPlayer(playerId, LocalDate.now());

        Double remainingMoneyToAllocate = totalPaymentAmount;

        for (Installment inst : overdueList) {
            if (remainingMoneyToAllocate <= 0) break;

            Double pendingOnInstallment = inst.getRemainingAmount();

            // Pay the full pending amount OR whatever money is left
            Double amountToPayHere = Math.min(pendingOnInstallment, remainingMoneyToAllocate);

            // 2. Create Payment Record
            Payment payment = new Payment();
            payment.setInstallment(inst);
            payment.setAmount(amountToPayHere);
            payment.setPaidOn(LocalDateTime.now());
            payment.setPaymentMethod(method != null ? method : "Bulk Overdue Payment");
            paymentRepository.save(payment);

            // 3. Update Installment Status
            double newPaid = (inst.getPaidAmount() == null ? 0.0 : inst.getPaidAmount()) + amountToPayHere;
            inst.setPaidAmount(newPaid);

            double newRemaining = (inst.getRemainingAmount() == null ? inst.getAmount() : inst.getRemainingAmount()) - amountToPayHere;
            if (newRemaining < 0.01) newRemaining = 0.0; // fix float precision

            inst.setRemainingAmount(newRemaining);

            if (newRemaining == 0.0) {
                inst.setStatus(Installment.Status.PAID);
            } else {
                inst.setStatus(Installment.Status.PARTIALLY_PAID);
            }

            installmentRepository.save(inst);

            remainingMoneyToAllocate -= amountToPayHere;
        }
    }
}
