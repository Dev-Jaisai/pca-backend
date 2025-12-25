package com.pca.service;

import com.pca.model.*;
import com.pca.repository.InstallmentRepository;
import com.pca.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerLifecycleService {

    private final PlayerRepository playerRepository;
    private final InstallmentRepository installmentRepository;

    // Dependency needed for creating fresh bills on activation
    private final InstallmentService installmentService;
    @Transactional
    public void pausePlayer(Long playerId, LocalDate startDate, String reason) {
        log.info("Pausing player {} from date {}", playerId, startDate);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        player.setIsActive(false);
        playerRepository.save(player);

        int billingDay = player.getBillingDay();

        // Calculate which billing cycle the holiday falls into
        // The cycle that CONTAINS the holiday date
        LocalDate cycleStartDate;

        int holidayDay = startDate.getDayOfMonth();

        if (holidayDay >= billingDay) {
            // Holiday is on/after billing day of this month
            // → Falls in cycle starting THIS month
            cycleStartDate = LocalDate.of(startDate.getYear(), startDate.getMonthValue(), billingDay);
        } else {
            // Holiday is before billing day of this month
            // → Falls in cycle starting LAST month
            cycleStartDate = LocalDate.of(startDate.getYear(), startDate.getMonthValue(), billingDay)
                    .minusMonths(1);
        }

        // The due date of this cycle = cycle start + 1 month
        LocalDate cycleDueDate = cycleStartDate.plusMonths(1);

        int targetMonth = cycleDueDate.getMonthValue();
        int targetYear = cycleDueDate.getYear();

        log.info("Holiday {} falls in cycle starting {} with due date {} → periodMonth={}, periodYear={}",
                startDate, cycleStartDate, cycleDueDate, targetMonth, targetYear);

        List<Installment> bills = installmentRepository.findByPlayerIdAndPeriodMonthAndPeriodYear(
                playerId, targetMonth, targetYear
        );

        if (!bills.isEmpty()) {
            skipBill(bills.get(0), reason);
        } else {
            createGhostBill(player, targetMonth, targetYear, cycleDueDate, reason);
        }
    }
    private void skipBill(Installment bill, String reason) {
        // Only skip if NOT PAID
        if (bill.getStatus() != Installment.Status.PAID) {
            bill.setStatus(Installment.Status.SKIPPED);
            bill.setNotes("Holiday/Paused: " + reason);
            bill.setAmount(0.0);
            bill.setRemainingAmount(0.0);
            installmentRepository.save(bill);
            log.info("✅ SKIPPED Bill ID: {} for Period: {}/{}", bill.getId(), bill.getPeriodMonth(), bill.getPeriodYear());
        } else {
            log.warn("⚠️ Cannot skip Bill ID: {} because it is already PAID.", bill.getId());
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
        log.info("👻 Created Ghost Bill for {}/{}", month, year);
    }
    @Transactional
    public void activatePlayer(Long playerId, LocalDate newBillingStartDate) {
        log.info("Activating player {} with new Start Date {}", playerId, newBillingStartDate);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        // 1. Set Active & New Billing Day
        // 🔥 हे लॉजिक बरोबर आहे: ज्या दिवशी Resume कराल, तोच नवीन Billing Day होईल.
        player.setIsActive(true);
        player.setBillingDay(newBillingStartDate.getDayOfMonth());
        playerRepository.save(player);

        // 2. Check & Delete SKIPPED bill if exists
        int month = newBillingStartDate.getMonthValue();
        int year = newBillingStartDate.getYear();

        List<Installment> existingBills = installmentRepository.findByPlayerIdAndPeriodMonthAndPeriodYear(
                playerId, month, year
        );

        if (!existingBills.isEmpty()) {
            Installment bill = existingBills.get(0);
            if (bill.getStatus() == Installment.Status.SKIPPED) {
                log.info("Deleting existing SKIPPED bill to make space for new active bill.");
                installmentRepository.delete(bill);
                installmentRepository.flush();
            }
        }

        // 🔥 3. Calculate Due Date (+1 Month Logic)
        // Resume Date = Start Date (09 Jun)
        // Due Date = 09 Jul (Next Month)
        int cycleMonths = (player.getPaymentCycleMonths() != null && player.getPaymentCycleMonths() > 0)
                ? player.getPaymentCycleMonths()
                : 1;

        LocalDate calculatedDueDate = newBillingStartDate.plusMonths(cycleMonths);

        // 4. Generate Immediate Bill
        installmentService.createInstallmentForPlayer(
                playerId,
                month, // Period Month (Jun)
                year,  // Period Year (2026)
                calculatedDueDate, // 🔥 CHANGE: Due Date is now +1 Month (09 Jul)
                null
        );

        log.info("Generated Welcome Back bill. Start: {}, Due: {}", newBillingStartDate, calculatedDueDate);
    }
}