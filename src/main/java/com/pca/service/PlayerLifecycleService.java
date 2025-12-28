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

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerLifecycleService {

    private final PlayerRepository playerRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentRepository paymentRepository;
    private final InstallmentService installmentService;

    // --- PAUSE PLAYER ---
    @Transactional
    public void pausePlayer(Long playerId, LocalDate startDate, String reason, Double advanceAmount) {
        log.info("Pausing player {} from date {}. Advance: {}", playerId, startDate, advanceAmount);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        player.setIsActive(false);

        if (advanceAmount != null && advanceAmount > 0) {
            Double currentCredit = player.getCreditBalance() != null ? player.getCreditBalance() : 0.0;
            player.setCreditBalance(currentCredit + advanceAmount);
        }
        playerRepository.save(player);

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
                playerId, targetMonth, targetYear);

        if (!bills.isEmpty()) {
            skipBill(bills.get(0), reason);
        } else {
            createGhostBill(player, targetMonth, targetYear, cycleDueDate, reason);
        }
    }

    // --- ACTIVATE PLAYER ---
    @Transactional
    public void activatePlayer(Long playerId, LocalDate newBillingStartDate) {
        log.info("Activating player {} with Start Date {}", playerId, newBillingStartDate);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        player.setIsActive(true);
        player.setBillingDay(newBillingStartDate.getDayOfMonth());

        int month = newBillingStartDate.getMonthValue();
        int year = newBillingStartDate.getYear();

        List<Installment> existingBills = installmentRepository
                .findByPlayerIdAndPeriodMonthAndPeriodYear(playerId, month, year);

        for (Installment existingBill : existingBills) {
            if (existingBill.getStatus() == Installment.Status.SKIPPED) {
                installmentRepository.delete(existingBill);
                log.info("Deleted SKIPPED bill {} for month {}/{}", existingBill.getId(), month, year);
            } else if (existingBill.getStatus() == Installment.Status.PENDING
                    || existingBill.getStatus() == Installment.Status.OVERDUE) {
                log.warn("Bill already exists for month {}/{}. Updating existing bill.", month, year);

                int cycleMonths = (player.getPaymentCycleMonths() != null && player.getPaymentCycleMonths() > 0)
                        ? player.getPaymentCycleMonths() : 1;
                LocalDate newDueDate = newBillingStartDate.plusMonths(cycleMonths);

                existingBill.setDueDate(newDueDate);
                existingBill.setNotes((existingBill.getNotes() != null ? existingBill.getNotes() : "")
                        + " | Reactivated on " + LocalDate.now());

                if (newDueDate.isBefore(LocalDate.now())) {
                    existingBill.setStatus(Installment.Status.OVERDUE);
                } else {
                    existingBill.setStatus(Installment.Status.PENDING);
                }

                installmentRepository.save(existingBill);
                playerRepository.save(player);
                return;
            } else if (existingBill.getStatus() == Installment.Status.PAID) {
                log.info("Bill for {}/{} already PAID. Will create next month's bill.", month, year);
                createNextMonthBill(player, month, year);
                playerRepository.save(player);
                return;
            }
        }

        installmentRepository.flush();

        int cycleMonths = (player.getPaymentCycleMonths() != null && player.getPaymentCycleMonths() > 0)
                ? player.getPaymentCycleMonths() : 1;

        LocalDate calculatedDueDate = newBillingStartDate.plusMonths(cycleMonths);
        createInstallmentWithCreditApplied(player, month, year, calculatedDueDate);
        playerRepository.save(player);
    }

    private void createNextMonthBill(Player player, int currentMonth, int currentYear) {
        LocalDate nextMonthDate = LocalDate.of(currentYear, currentMonth, player.getBillingDay()).plusMonths(1);
        int nextMonth = nextMonthDate.getMonthValue();
        int nextYear = nextMonthDate.getYear();

        boolean exists = installmentRepository.existsByPlayerIdAndPeriodMonthAndPeriodYear(
                player.getId(), nextMonth, nextYear);

        if (!exists) {
            int cycleMonths = (player.getPaymentCycleMonths() != null && player.getPaymentCycleMonths() > 0)
                    ? player.getPaymentCycleMonths() : 1;
            LocalDate dueDate = nextMonthDate.plusMonths(cycleMonths);

            createInstallmentWithCreditApplied(player, nextMonth, nextYear, dueDate);
            log.info("Created next month's bill for {}/{}", nextMonth, nextYear);
        }
    }

    private void createInstallmentWithCreditApplied(Player player, int month, int year, LocalDate dueDate) {
        Double baseAmount = 5000.0;
        Double creditAvailable = player.getCreditBalance() != null ? player.getCreditBalance() : 0.0;
        Double creditToApply = Math.min(creditAvailable, baseAmount);
        Double finalAmount = baseAmount - creditToApply;

        Installment.Status initialStatus = dueDate.isBefore(LocalDate.now())
                ? Installment.Status.OVERDUE : Installment.Status.PENDING;

        Installment ins = Installment.builder()
                .player(player)
                .periodMonth(month)
                .periodYear(year)
                .amount(finalAmount)
                .paidAmount(0.0)
                .remainingAmount(finalAmount)
                .status(initialStatus)
                .dueDate(dueDate)
                .notes(creditToApply > 0 ? String.format("Credit Applied: ₹%.0f (Balance: ₹%.0f)",
                        creditToApply, baseAmount) : null)
                .build();

        installmentRepository.save(ins);

        player.setCreditBalance(creditAvailable - creditToApply);
        playerRepository.save(player);
    }

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
    public String markPlayerLeft(Long playerId, LocalDate leftDate, LeftOption option, Double manualAmount) {
        log.info("Marking player {} LEFT from date {}. Option: {}", playerId, leftDate, option);

        String responseMessage = "Player marked as LEFT successfully.";

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        int billingDay = player.getBillingDay();

        // Calculate Billing Cycle
        LocalDate cycleStartDate;
        int leftDay = leftDate.getDayOfMonth();

        if (leftDay >= billingDay) {
            cycleStartDate = LocalDate.of(leftDate.getYear(), leftDate.getMonthValue(), billingDay);
        } else {
            cycleStartDate = LocalDate.of(leftDate.getYear(), leftDate.getMonthValue(), billingDay).minusMonths(1);
        }

        LocalDate cycleDueDate = cycleStartDate.plusMonths(1);
        int targetMonth = cycleDueDate.getMonthValue();
        int targetYear = cycleDueDate.getYear();

        log.info("Left Date: {}, Cycle: {} to {}, Target Period: {}/{}",
                leftDate, cycleStartDate, cycleDueDate, targetMonth, targetYear);

        // 1. Safety Check
        List<Installment> futurePaidBills = installmentRepository.findFuturePaidBills(
                playerId, targetMonth, targetYear);
        if (!futurePaidBills.isEmpty()) {
            throw new RuntimeException("Warning: Paid bills exist after " + leftDate + ". Refund manually first.");
        }

        // 2. Find Target Bill
        Installment targetBill = null;
        List<Installment> currentCycleBills = installmentRepository
                .findByPlayerIdAndPeriodMonthAndPeriodYear(playerId, targetMonth, targetYear);

        if (!currentCycleBills.isEmpty()) {
            Installment candidate = currentCycleBills.get(0);

            // 🔥 Check if we are updating a PAID bill
            boolean isUpdateScenario = (option == LeftOption.COLLECT_PARTIAL);

            if (candidate.getStatus() != Installment.Status.PAID || isUpdateScenario) {
                targetBill = candidate;
                log.info("Found Target Bill (Current Cycle): ID={}, Status={}", targetBill.getId(), targetBill.getStatus());
            }
        }

        if (targetBill == null) {
            targetBill = installmentRepository
                    .findFirstByPlayerIdAndDueDateAfterAndStatusNotOrderByDueDateAsc(
                            playerId, leftDate, Installment.Status.PAID);
        }

        // 3. Process Bill
        if (targetBill != null) {
            log.info("Processing Target Bill: {} (Due: {})", targetBill.getId(), targetBill.getDueDate());

            switch (option) {
                case COLLECT_FULL:
                    targetBill.setNotes((targetBill.getNotes() != null ? targetBill.getNotes() : "")
                            + " | Student Left on " + leftDate + ". Full Fee Charged.");
                    break;

                case COLLECT_PARTIAL:
                    if (manualAmount != null && manualAmount >= 0) {
                        Double oldAmount = targetBill.getAmount();
                        Double alreadyPaid = targetBill.getPaidAmount();

                        targetBill.setAmount(manualAmount);
                        double newRemaining = manualAmount - alreadyPaid;

                        String refundNote = "";

                        if (newRemaining <= 0) {
                            // Refund Case
                            targetBill.setRemainingAmount(0.0);
                            targetBill.setStatus(Installment.Status.PAID);

                            if (newRemaining < 0) {
                                double refund = Math.abs(newRemaining);
                                // 🔥 Set Response & Note for Refund
                                responseMessage = String.format("⚠️ YOU NEED TO REFUND ₹%.0f", refund);
                                refundNote = String.format(" | ⚠️ REFUND DUE: ₹%.0f to Student.", refund);
                            } else {
                                responseMessage = "Fee Updated. Balance Settled.";
                            }
                        } else {
                            // Pending Case
                            targetBill.setRemainingAmount(newRemaining);
                            targetBill.setStatus(targetBill.getDueDate().isBefore(LocalDate.now())
                                    ? Installment.Status.OVERDUE
                                    : Installment.Status.PENDING);

                            // Ensure it goes back to PENDING if it was PAID
                            if (targetBill.getStatus() == Installment.Status.PAID) {
                                targetBill.setStatus(Installment.Status.PENDING);
                            }

                            responseMessage = String.format("Fee Updated. Collect Remaining ₹%.0f", newRemaining);
                        }

                        String note = String.format(" | Student Left on %s. Fee Updated: %.0f -> %.0f (Partial)%s",
                                leftDate, oldAmount, manualAmount, refundNote);
                        targetBill.setNotes((targetBill.getNotes() != null ? targetBill.getNotes() : "") + note);
                    }
                    break;

                case WAIVE_OFF:
                    targetBill.setAmount(0.0);
                    targetBill.setRemainingAmount(0.0);
                    targetBill.setStatus(Installment.Status.SKIPPED);
                    targetBill.setNotes("Student Left on " + leftDate + ". Waived Off.");
                    responseMessage = "Bill Waived Off.";
                    break;
            }
            installmentRepository.save(targetBill);

            // B. Cancel Future Bills (Exclude Target Bill)
            LocalDate cancelStartDate = targetBill.getDueDate().plusDays(1);
            installmentService.cancelFutureBills(playerId, cancelStartDate, targetBill.getId());

        } else {
            // No target bill found, cancel next month onwards
            LocalDate cancelStartDate = leftDate.plusDays(1);
            installmentService.cancelFutureBills(playerId, cancelStartDate, -1L);
        }

        // 4. Inactivate Player
        player.setIsActive(false);
        player.setNotes((player.getNotes() != null ? player.getNotes() : "") + " | LEFT: " + leftDate);
        playerRepository.save(player);

        log.info("Player {} marked as LEFT. Response: {}", playerId, responseMessage);
        return responseMessage; // 🔥 Return Message to UI
    }

    // 🔥🔥🔥 UNDO PLAYER LEFT (Partial Bill Revert + No Auto-Generate) 🔥🔥🔥
    @Transactional
    public void undoPlayerLeft(Long playerId) {
        log.info("Undoing LEFT status for player {}", playerId);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        player.setIsActive(true);
        player.setNotes((player.getNotes() != null ? player.getNotes() : "") +
                " | Left Status Undone on " + LocalDate.now());
        playerRepository.save(player);

        // 🔥 FIX 1: PARTIAL AMOUNT चे bills revert करा
        List<Installment> allBills = installmentRepository.findByPlayerId(playerId);

        int partialRevertedCount = 0;
        for (Installment bill : allBills) {
            if (bill.getNotes() != null &&
                    bill.getNotes().contains("Student Left") &&
                    bill.getNotes().contains("Fee Updated")) {

                String notes = bill.getNotes();
                if (notes.contains("Fee Updated:")) {
                    try {
                        int startIdx = notes.indexOf("Fee Updated:") + 13;
                        int arrowIdx = notes.indexOf("->", startIdx);
                        String originalAmtStr = notes.substring(startIdx, arrowIdx).trim();
                        Double originalAmount = Double.parseDouble(originalAmtStr);

                        bill.setAmount(originalAmount);
                        bill.setRemainingAmount(originalAmount - bill.getPaidAmount());

                        if (bill.getRemainingAmount() <= 0) {
                            bill.setStatus(Installment.Status.PAID);
                        } else if (bill.getDueDate().isBefore(LocalDate.now())) {
                            bill.setStatus(Installment.Status.OVERDUE);
                        } else {
                            bill.setStatus(Installment.Status.PENDING);
                        }

                        bill.setNotes(notes + " | Amount Reverted (Undo Left)");
                        installmentRepository.save(bill);
                        partialRevertedCount++;

                        log.info("Reverted partial bill {} from {} to {}",
                                bill.getId(), bill.getAmount(), originalAmount);
                    } catch (Exception e) {
                        log.warn("Could not parse original amount, using default 5000");
                        bill.setAmount(5000.0);
                        bill.setRemainingAmount(5000.0 - bill.getPaidAmount());
                        bill.setNotes(notes + " | Amount Reverted to Default 5000 (Undo Left)");
                        installmentRepository.save(bill);
                        partialRevertedCount++;
                    }
                }
            }
        }

        // 🔥 FIX 2: CANCELLED bills restore करा
        List<Installment> cancelledBills = installmentRepository
                .findByPlayerIdAndStatus(playerId, Installment.Status.CANCELLED);

        int restoredCount = 0;
        for (Installment bill : cancelledBills) {
            if (bill.getNotes() != null &&
                    (bill.getNotes().contains("Player Left") ||
                            bill.getNotes().contains("Auto-cancelled"))) {

                Double originalAmount = bill.getAmount();
                if (originalAmount == null || originalAmount == 0.0) {
                    originalAmount = 5000.0;
                }

                bill.setStatus(bill.getDueDate().isBefore(LocalDate.now())
                        ? Installment.Status.OVERDUE
                        : Installment.Status.PENDING);
                bill.setAmount(originalAmount);
                bill.setRemainingAmount(originalAmount - bill.getPaidAmount());
                bill.setNotes((bill.getNotes() != null ? bill.getNotes() : "") +
                        " | Restored (Undo Left)");

                installmentRepository.save(bill);
                restoredCount++;
            }
        }

        // 🔥 FIX 3: SKIPPED (Waived Off) bills restore करा
        List<Installment> skippedBills = installmentRepository
                .findByPlayerIdAndStatus(playerId, Installment.Status.SKIPPED);

        int unskippedCount = 0;
        for (Installment bill : skippedBills) {
            if (bill.getNotes() != null &&
                    bill.getNotes().contains("Waived Off") &&
                    bill.getNotes().contains("Student Left")) {

                bill.setStatus(Installment.Status.PENDING);
                bill.setAmount(5000.0);
                bill.setRemainingAmount(5000.0);
                bill.setNotes(bill.getNotes() + " | Un-waived (Undo Left)");

                installmentRepository.save(bill);
                unskippedCount++;
            }
        }

        // 🔥 REMOVED: Auto-generation (User doesn't want it!)
        // createNextBillForPlayer(player); ← DELETED

        log.info("Player {} LEFT status undone. Reverted {} partial, Restored {} cancelled, Un-waived {} skipped",
                playerId, partialRevertedCount, restoredCount, unskippedCount);
    }
}