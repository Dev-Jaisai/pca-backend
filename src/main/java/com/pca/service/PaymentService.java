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
import java.util.Comparator;
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
        try {
            log.info("Starting overdue payment for player {} amount {}", playerId, totalPaymentAmount);

            // 1. Get overdue installments
            List<Installment> overdueList = installmentRepository
                    .findOverdueInstallmentsByPlayer(playerId, LocalDate.now());

            log.info("Found {} overdue installments", overdueList.size());

            if (overdueList.isEmpty()) {
                log.warn("No overdue installments found for player {}", playerId);
                return;
            }

            Double remainingMoneyToAllocate = totalPaymentAmount;

            for (Installment inst : overdueList) {
                if (remainingMoneyToAllocate <= 0) break;

                Double pendingOnInstallment = inst.getRemainingAmount();
                Double amountToPayHere = Math.min(pendingOnInstallment, remainingMoneyToAllocate);

                log.info("Paying installment {}: amount {}", inst.getId(), amountToPayHere);

                // 2. Create Payment Record
                Payment payment = Payment.builder()
                        .installment(inst)
                        .amount(amountToPayHere)
                        .paidOn(LocalDateTime.now())
                        .paymentMethod(method != null ? method : "Cash")
                        .build();

                Payment savedPayment = paymentRepository.save(payment);
                log.info("Payment record created with ID: {}", savedPayment.getId());

                // 3. Update Installment
                double newPaid = (inst.getPaidAmount() == null ? 0.0 : inst.getPaidAmount()) + amountToPayHere;
                inst.setPaidAmount(newPaid);

                double newRemaining = inst.getAmount() - newPaid;
                if (newRemaining < 0.01) newRemaining = 0.0;
                inst.setRemainingAmount(newRemaining);

                if (newRemaining == 0.0) {
                    inst.setStatus(Installment.Status.PAID);
                } else {
                    inst.setStatus(Installment.Status.PARTIALLY_PAID);
                }

                installmentRepository.save(inst);
                log.info("Updated installment {}: paid={}, remaining={}, status={}",
                        inst.getId(), newPaid, newRemaining, inst.getStatus());

                remainingMoneyToAllocate -= amountToPayHere;
            }

            log.info("Overdue payment completed for player {}", playerId);

        } catch (Exception e) {
            log.error("Error in payOverdue for player {}: {}", playerId, e.getMessage(), e);
            throw new RuntimeException("Failed to process overdue payment: " + e.getMessage());
        }
    }
    /**
     * Common payment processing logic for both overdue and unpaid
     */
    private void processPaymentList(Long playerId, Double totalPaymentAmount, String method,
                                    List<Installment> installments, String paymentType) {

        Double remainingMoneyToAllocate = totalPaymentAmount;

        for (Installment inst : installments) {
            if (remainingMoneyToAllocate <= 0) break;

            // Get remaining amount
            Double pendingOnInstallment = inst.getRemainingAmount();
            if (pendingOnInstallment == null || pendingOnInstallment <= 0) {
                continue; // Already paid (should not happen, but safe check)
            }

            Double amountToPayHere = Math.min(pendingOnInstallment, remainingMoneyToAllocate);

            log.info("Paying {} installment {} (due {}): amount {}",
                    paymentType, inst.getId(), inst.getDueDate(), amountToPayHere);

            // Create Payment Record
            Payment payment = Payment.builder()
                    .installment(inst)
                    .amount(amountToPayHere)
                    .paidOn(LocalDateTime.now())
                    .paymentMethod(method != null ? method : "Cash")
                    .build();

            Payment savedPayment = paymentRepository.save(payment);
            log.info("Payment record created with ID: {}", savedPayment.getId());

            // Update Installment
            double newPaid = (inst.getPaidAmount() == null ? 0.0 : inst.getPaidAmount()) + amountToPayHere;
            inst.setPaidAmount(newPaid);

            double newRemaining = inst.getAmount() - newPaid;
            if (newRemaining < 0.01) newRemaining = 0.0;
            inst.setRemainingAmount(newRemaining);

            // Update status
            if (newRemaining == 0.0) {
                inst.setStatus(Status.PAID);
            } else if (newPaid > 0.0) {
                inst.setStatus(Status.PARTIALLY_PAID);
            }

            installmentRepository.save(inst);
            log.info("Updated installment {}: paid={}, remaining={}, status={}",
                    inst.getId(), newPaid, newRemaining, inst.getStatus());

            remainingMoneyToAllocate -= amountToPayHere;
        }

        log.info("{} payment completed for player {}. Remaining unallocated: {}",
                paymentType, playerId, remainingMoneyToAllocate);
    }
    // NEW: Pay all unpaid installments (overdue + pending + future)
    @Transactional
    public void payUnpaid(Long playerId, Double totalPaymentAmount, String method) {
        log.info("Starting ALL UNPAID payment for player {} amount {}", playerId, totalPaymentAmount);

        // Get all unpaid installments (status != PAID)
        List<Installment> unpaidList = installmentRepository.findByPlayerIdAndStatusNot(playerId, Status.PAID);

        // Sort by due date (oldest first)
        unpaidList.sort(Comparator.comparing(Installment::getDueDate));

        log.info("Found {} unpaid installments", unpaidList.size());

        if (unpaidList.isEmpty()) {
            log.warn("No unpaid installments found for player {}", playerId);
            return;
        }

        processPaymentList(playerId, totalPaymentAmount, method, unpaidList, "all unpaid");
    }


}
