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
    private final PaymentRepository paymentRepository; // (Not used for delete anymore, but kept if needed)

    // 🔥 Used for Cancel Logic & Creating Bills
    private final InstallmentService installmentService;

    // --- PAUSE PLAYER (Unchanged) ---
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
                playerId, targetMonth, targetYear
        );

        if (!bills.isEmpty()) {
            skipBill(bills.get(0), reason);
        } else {
            createGhostBill(player, targetMonth, targetYear, cycleDueDate, reason);
        }
    }

    // --- ACTIVATE PLAYER (Unchanged) ---
    @Transactional
    public void activatePlayer(Long playerId, LocalDate newBillingStartDate) {
        log.info("Activating player {} with Start Date {}", playerId, newBillingStartDate);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        player.setIsActive(true);
        player.setBillingDay(newBillingStartDate.getDayOfMonth());

        int month = newBillingStartDate.getMonthValue();
        int year = newBillingStartDate.getYear();

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

        int cycleMonths = (player.getPaymentCycleMonths() != null && player.getPaymentCycleMonths() > 0)
                ? player.getPaymentCycleMonths()
                : 1;

        LocalDate calculatedDueDate = newBillingStartDate.plusMonths(cycleMonths);
        createInstallmentWithCreditApplied(player, month, year, calculatedDueDate);
        playerRepository.save(player);
    }

    private void createInstallmentWithCreditApplied(Player player, int month, int year, LocalDate dueDate) {
        Double baseAmount = 5000.0;
        Double creditAvailable = player.getCreditBalance() != null ? player.getCreditBalance() : 0.0;
        Double creditToApply = Math.min(creditAvailable, baseAmount);
        Double finalAmount = baseAmount - creditToApply;

        Installment.Status initialStatus = dueDate.isBefore(LocalDate.now())
                ? Installment.Status.OVERDUE
                : Installment.Status.PENDING;

        Installment ins = Installment.builder()
                .player(player)
                .periodMonth(month)
                .periodYear(year)
                .amount(finalAmount)
                .paidAmount(0.0)
                .remainingAmount(finalAmount)
                .status(initialStatus)
                .dueDate(dueDate)
                .notes(creditToApply > 0
                        ? String.format("Credit Applied: ₹%.0f (Balance: ₹%.0f)", creditToApply, baseAmount)
                        : null)
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

    // 🔥🔥🔥 MARK PLAYER LEFT (UPDATED TO CANCEL INSTEAD OF DELETE) 🔥🔥🔥
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

        // 🔥🔥🔥 FIX START: Target Bill शोधण्याचे नवीन लॉजिक 🔥🔥🔥
        Installment targetBill = null;

        // Step A: आधी 'Left Date' च्या महिन्याचे बिल शोधा (उदा. Sep बिल)
        // कारण Due Date (1 Sep) ही Left Date (12 Sep) च्या आधी असू शकते.
        List<Installment> currentMonthBills = installmentRepository.findByPlayerIdAndPeriodMonthAndPeriodYear(
                playerId, leftDate.getMonthValue(), leftDate.getYear()
        );

        if (!currentMonthBills.isEmpty()) {
            Installment candidate = currentMonthBills.get(0);
            // जर हे बिल PAID नसेल, तर हेच आपले Target Bill आहे.
            if (candidate.getStatus() != Installment.Status.PAID) {
                targetBill = candidate;
                log.info("Found Target Bill (Current Month): {}", targetBill.getId());
            }
        }

        // Step B: जर चालू महिन्याचे बिल सापडले नाही (किंवा पेड असेल), तर पुढचे बिल शोधा
        if (targetBill == null) {
            targetBill = installmentRepository.findFirstByPlayerIdAndDueDateAfterAndStatusNotOrderByDueDateAsc(
                    playerId, leftDate, Installment.Status.PAID
            );
        }
        // 🔥🔥🔥 FIX END 🔥🔥🔥

        if (targetBill != null) {
            log.info("Processing Target Bill: {} (Due: {})", targetBill.getId(), targetBill.getDueDate());

            // A. Update Target Bill
            switch (option) {
                case COLLECT_FULL:
                    targetBill.setNotes("Student Left on " + leftDate + ". Full Fee Charged.");
                    break;
                case COLLECT_PARTIAL:
                    if (manualAmount != null && manualAmount >= 0) {
                        Double oldAmount = targetBill.getAmount();
                        targetBill.setAmount(manualAmount);
                        targetBill.setRemainingAmount(manualAmount - targetBill.getPaidAmount());

                        // Detailed Note
                        String note = String.format(" | Student Left on %s. Fee Reduced: %.0f -> %.0f (Refund Diff: %.0f)",
                                leftDate, oldAmount, manualAmount, (oldAmount - manualAmount));

                        targetBill.setNotes((targetBill.getNotes() != null ? targetBill.getNotes() : "") + note);
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

            // B. SAFE CANCEL (Future Bills)
            // 🔥 आता आपण Target Bill च्या Due Date नंतरची सर्व बिले कॅन्सल करतोय.
            // Sep (Due 1 Sep) टारगेट असेल, तर Oct (1 Oct), Nov (1 Nov)... सर्व कॅन्सल होतील.
            installmentService.cancelFutureBills(playerId, targetBill.getDueDate());

        } else {
            // No target bill found, cancel everything after Left Date
            installmentService.cancelFutureBills(playerId, leftDate);
        }

        // 3. Mark Inactive
        player.setIsActive(false);
        player.setNotes(player.getNotes() + " | LEFT: " + leftDate);
        playerRepository.save(player);
    }

    // ❌ performSafeDelete function is REMOVED completely.
}