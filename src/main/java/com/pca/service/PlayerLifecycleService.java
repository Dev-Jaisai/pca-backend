package com.pca.service;

import com.pca.enums.LeftOption;
import com.pca.model.*;
import com.pca.repository.InstallmentRepository;
import com.pca.repository.PaymentRepository;
import com.pca.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.pca.enums.LeftOption.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerLifecycleService {

    private final PlayerRepository playerRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentRepository paymentRepository;
    // Dependency needed for creating fresh bills on activation
    private final InstallmentService installmentService;

    // 🔥 UPDATED METHOD SIGNATURE (Accept Advance Amount)
    @Transactional
    public void pausePlayer(Long playerId, LocalDate startDate, String reason, Double advanceAmount) {
        log.info("Pausing player {} from date {}. Advance: {}", playerId, startDate, advanceAmount);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        // 1. Set Inactive
        player.setIsActive(false);

        // 🔥 2. Save Advance Credit (If Provided)
        if (advanceAmount != null && advanceAmount > 0) {
            Double currentCredit = player.getCreditBalance() != null ? player.getCreditBalance() : 0.0;
            player.setCreditBalance(currentCredit + advanceAmount);
            log.info("Added ₹{} to credit. New balance: ₹{}", advanceAmount, player.getCreditBalance());
        }

        playerRepository.save(player);

        // 3. Skip Current Cycle Bill (Same Logic as Before)
        int billingDay = player.getBillingDay();
        LocalDate cycleStartDate;

        int holidayDay = startDate.getDayOfMonth();

        if (holidayDay >= billingDay) {
            cycleStartDate = LocalDate.of(startDate.getYear(), startDate.getMonthValue(), billingDay);
        } else {
            cycleStartDate = LocalDate.of(startDate.getYear(), startDate.getMonthValue(), billingDay).minusMonths(1);
        }

        LocalDate cycleDueDate = cycleStartDate.plusMonths(1);
        int targetMonth = cycleDueDate.getMonthValue();
        int targetYear = cycleDueDate.getYear();

        List<Installment> bills = installmentRepository.findByPlayerIdAndPeriodMonthAndPeriodYear(
                playerId, targetMonth, targetYear
        );

        if (!bills.isEmpty()) {
            skipBill(bills.get(0), reason);
        } else {
            createGhostBill(player, targetMonth, targetYear, cycleDueDate, reason);
        }
    }

    // 🔥 UPDATED ACTIVATE METHOD (Apply Credit)
    @Transactional
    public void activatePlayer(Long playerId, LocalDate newBillingStartDate) {
        log.info("Activating player {} with Start Date {}", playerId, newBillingStartDate);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        player.setIsActive(true);
        player.setBillingDay(newBillingStartDate.getDayOfMonth());

        int month = newBillingStartDate.getMonthValue();
        int year = newBillingStartDate.getYear();

        // Delete existing SKIPPED bill if exists
        List<Installment> existingBills = installmentRepository.findByPlayerIdAndPeriodMonthAndPeriodYear(
                playerId, month, year
        );

        if (!existingBills.isEmpty()) {
            Installment bill = existingBills.get(0);
            if (bill.getStatus() == Installment.Status.SKIPPED) {
                installmentRepository.delete(bill);
                installmentRepository.flush();
            }
        }

        // Calculate Due Date
        int cycleMonths = (player.getPaymentCycleMonths() != null && player.getPaymentCycleMonths() > 0)
                ? player.getPaymentCycleMonths()
                : 1;

        LocalDate calculatedDueDate = newBillingStartDate.plusMonths(cycleMonths);

        // 🔥🔥🔥 CREATE BILL WITH CREDIT APPLIED 🔥🔥🔥
        createInstallmentWithCreditApplied(player, month, year, calculatedDueDate);

        playerRepository.save(player);
    }

    // 🔥🔥🔥 NEW HELPER METHOD: CREATE BILL + APPLY CREDIT 🔥🔥🔥
    private void createInstallmentWithCreditApplied(Player player, int month, int year, LocalDate dueDate) {
        // 1. Get Base Amount (From Fee Structure)
        Double baseAmount = 5000.0; // Replace with dynamic fee lookup if needed
        // Example: feeStructureService.findEffectiveFeeForGroup(player.getPlayerGroup(), LocalDate.now()).getMonthlyFee();

        Double creditAvailable = player.getCreditBalance() != null ? player.getCreditBalance() : 0.0;

        // 2. Calculate Final Amount
        Double creditToApply = Math.min(creditAvailable, baseAmount);
        Double finalAmount = baseAmount - creditToApply;

        // 3. Create Installment
        Installment.Status initialStatus = dueDate.isBefore(LocalDate.now())
                ? Installment.Status.OVERDUE
                : Installment.Status.PENDING;

        Installment ins = Installment.builder()
                .player(player)
                .periodMonth(month)
                .periodYear(year)
                .amount(finalAmount) // 🔥 Adjusted Amount
                .paidAmount(0.0)
                .remainingAmount(finalAmount)
                .status(initialStatus)
                .dueDate(dueDate)
                .notes(creditToApply > 0
                        ? String.format("Credit Applied: ₹%.0f (Balance: ₹%.0f)", creditToApply, baseAmount)
                        : null)
                .build();

        installmentRepository.save(ins);

        // 4. Deduct Credit from Player
        player.setCreditBalance(creditAvailable - creditToApply);
        playerRepository.save(player);

        log.info("Created bill: Base=₹{}, Credit=₹{}, Final=₹{}. Remaining Credit=₹{}",
                baseAmount, creditToApply, finalAmount, player.getCreditBalance());
    }

    // Existing Helper Methods (Unchanged)
    private void skipBill(Installment bill, String reason) {
        if (bill.getStatus() != Installment.Status.PAID) {
            bill.setStatus(Installment.Status.SKIPPED);
            bill.setNotes("Holiday/Paused: " + reason);
            bill.setAmount(0.0);
            bill.setRemainingAmount(0.0);
            installmentRepository.save(bill);
        }
    }

    private void createGhostBill(Player player, int month, int year, LocalDate date, String reason) {
        Installment ghostBill = Installment.builder()
                .player(player)
                .periodMonth(month)
                .periodYear(year)
                .dueDate(date)
                .amount(0.0)
                .paidAmount(0.0)
                .remainingAmount(0.0)
                .status(Installment.Status.SKIPPED)
                .notes("Pre-informed Holiday: " + reason)
                .build();
        installmentRepository.save(ghostBill);
    }
    @Transactional
    public void markPlayerLeft(Long playerId, LocalDate leftDate, com.pca.enums.LeftOption option, Double manualAmount) {
        log.info("Marking player {} LEFT from date {}. Option: {}", playerId, leftDate, option);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        // 1. Safety Check (Future Paid Bills)
        List<Installment> futurePaidBills = installmentRepository.findFuturePaidBills(playerId, leftDate);
        if (!futurePaidBills.isEmpty()) {
            throw new RuntimeException("Warning: Paid bills exist after " + leftDate + ". Refund manually first.");
        }

        // 🔥 2. FIND TARGET BILL (Next Due Bill after Left Date)
        Installment targetBill = installmentRepository.findFirstByPlayerIdAndDueDateAfterAndStatusNotOrderByDueDateAsc(
                playerId,
                leftDate,
                Installment.Status.PAID
        );

        if (targetBill != null) {
            log.info("Found Target Bill to Update: {} (Due: {})", targetBill.getId(), targetBill.getDueDate());

            // A. Update Target Bill
            switch (option) {
                case COLLECT_FULL:
                    targetBill.setNotes("Student Left on " + leftDate + ". Full Fee Charged.");
                    break;
                case COLLECT_PARTIAL:
                    if (manualAmount != null && manualAmount >= 0) {
                        targetBill.setAmount(manualAmount);
                        targetBill.setRemainingAmount(manualAmount - targetBill.getPaidAmount());
                        targetBill.setNotes("Student Left on " + leftDate + ". Partial Charge: " + manualAmount);
                    }
                    break;
                case WAIVE_OFF:
                    targetBill.setAmount(0.0);
                    targetBill.setRemainingAmount(0.0);
                    targetBill.setStatus(Installment.Status.SKIPPED);
                    targetBill.setNotes("Student Left on " + leftDate + ". Waived Off.");
                    break;
            }
            installmentRepository.save(targetBill);

            // B. SAFE DELETE (Future Bills)
            // 🔥 Pass targetBill DueDate to delete everything AFTER it
            performSafeDelete(playerId, targetBill.getDueDate());

        } else {
            // No target bill found, delete everything after Left Date
            performSafeDelete(playerId, leftDate);
        }

        // 3. Mark Inactive
        player.setIsActive(false);
        player.setNotes(player.getNotes() + " | LEFT: " + leftDate);
        playerRepository.save(player);
    }

    // 🔥🔥🔥 HELPER METHOD FOR SAFE DELETE 🔥🔥🔥
    private void performSafeDelete(Long playerId, LocalDate afterDate) {
        // 1. Find bills needed to be deleted
        List<Installment> billsToDelete = installmentRepository.findFuturePendingBills(playerId, afterDate);

        if (!billsToDelete.isEmpty()) {
            // 2. Extract IDs
            List<Long> billIds = billsToDelete.stream()
                    .map(Installment::getId)
                    .collect(Collectors.toList());

            // 3. Delete related Payments first (Using existing efficient method)
            paymentRepository.deleteByInstallmentIds(billIds);
            log.info("Deleted payments associated with {} future bills.", billIds.size());

            // 4. Now Delete the Bills
            installmentRepository.deleteAll(billsToDelete);
            log.info("Deleted {} future bills after date {}", billsToDelete.size(), afterDate);
        } else {
            log.info("No future bills found to delete after {}", afterDate);
        }
    }
}