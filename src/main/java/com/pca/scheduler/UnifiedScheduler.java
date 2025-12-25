package com.pca.scheduler;

import com.pca.model.Installment;
import com.pca.model.Player;
import com.pca.repository.InstallmentRepository;
import com.pca.repository.PlayerRepository;
import com.pca.service.InstallmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnifiedScheduler {

    private final InstallmentService installmentService;
    private final PlayerRepository playerRepository;
    private final InstallmentRepository installmentRepository;

    // ==============================================================================
    // THE ONE AND ONLY DAILY JOB
    // Runs every day at 12:05 AM (00:05:00)
    // ==============================================================================
// ❌ जुनी वेळ: @Scheduled(cron = "0 5 0 * * ?") (रात्री 12:05)
// ✅ नवीन वेळ (Testing): 3:26 PM (15:26)
    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional
    public void runDailyJobs() {
        log.info("=== 🌙 Starting Nightly Jobs (12:05 AM) ===");

        // STEP 1: Mark old unpaid bills as OVERDUE
        log.info(">> Step 1: Updating overdue statuses...");
        installmentService.updateOverdueStatuses();

        // STEP 2: Create NEW installments for the next month
        log.info(">> Step 2: Generating new monthly installments...");
        generateDailyInstallments();

        log.info("=== ✅ Nightly Jobs Completed ===");
    }

    /**
     * Logic:
     * 1. Find players whose "Billing Day" is today (or earlier in the month if we missed them).
     * 2. Check if enough time has passed since the LAST bill (based on Payment Cycle).
     * 3. If yes, create the new bill with a Strict Due Date.
     */
    private void generateDailyInstallments() {
        LocalDate today = LocalDate.now();
        int currentDay = today.getDayOfMonth();

        log.info("Running Advance Cycle Check for Day: {}", currentDay);

        // 1. फक्त बिलिंग डे वालेच नाही, तर 'सर्व' Active प्लेयर्स चेक करणे जास्त सुरक्षित आहे.
        // पण तुझ्या लॉजिकनुसार आपण बिलिंग डे वालेच घेऊ.
//        List<Player> players = playerRepository.findByBillingDayLessThanEqual(currentDay);
        List<Player> players = playerRepository.findActivePlayersByBillingDay(currentDay);
        for (Player p : players) {
            try {
                Installment lastInst = installmentRepository.findLastByPlayerId(p.getId());

                int cycleMonths = (p.getPaymentCycleMonths() != null && p.getPaymentCycleMonths() > 0)
                        ? p.getPaymentCycleMonths() : 1;

                boolean shouldGenerate = false;
                int targetMonth = 0;
                int targetYear = 0;

                if (lastInst == null) {
                    // पहिलाच हप्ता
                    shouldGenerate = true;
                    targetMonth = today.getMonthValue();
                    targetYear = today.getYear();
                } else {
                    YearMonth lastPeriod = YearMonth.of(lastInst.getPeriodYear(), lastInst.getPeriodMonth());
                    YearMonth nextTargetPeriod = lastPeriod.plusMonths(cycleMonths); // e.g. Jan 2026
                    YearMonth currentPeriod = YearMonth.from(today); // Dec 2025

                    // 🔥 MAJOR CHANGE HERE (ADVANCE LOGIC):
                    // जुनं: if (!today.isBefore(nextTriggerDate)) { ... }
                    // नवीन: "जर पुढचा हप्ता 'पुढच्या महिन्यात' येत असेल, तर तो आत्ताच बनवा."

                    // Logic: Is NextTarget <= (Current Month + 1)?
                    // उदा. Last=Dec. Next=Jan. Current=Dec.
                    // Jan <= (Dec+1)? YES! -> Generate Jan NOW.

                    if (!nextTargetPeriod.isAfter(currentPeriod.plusMonths(1)) ||
                            (lastInst.getDueDate() != null && lastInst.getDueDate().isBefore(today))) {
                        // Check Duplicate
                        boolean exists = installmentRepository.existsByPlayerIdAndPeriodMonthAndPeriodYear(
                                p.getId(), nextTargetPeriod.getMonthValue(), nextTargetPeriod.getYear());

                        if (!exists) {
                            shouldGenerate = true;
                            targetMonth = nextTargetPeriod.getMonthValue();
                            targetYear = nextTargetPeriod.getYear();
                        }
                    }
                }

                if (shouldGenerate) {
                    // Due Date Calculation
                    LocalDate targetDate = LocalDate.of(targetYear, targetMonth, 1);
                    int billDay = p.getBillingDay() != null ? p.getBillingDay() : 1;

                    int maxDays = targetDate.lengthOfMonth();
                    if (billDay > maxDays) billDay = maxDays;

                    LocalDate strictDueDate = LocalDate.of(targetYear, targetMonth, billDay);

                    installmentService.createInstallmentForPlayer(
                            p.getId(), targetMonth, targetYear, strictDueDate, null
                    );

                    log.info("ADVANCE Bill Generated for {}: Month={}/{}, Due={}",
                            p.getName(), targetMonth, targetYear, strictDueDate);
                }

            } catch (Exception e) {
                log.error("Error for {}: {}", p.getName(), e.getMessage());
            }
        }
    }
}