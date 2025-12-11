package com.pca.scheduler;

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
    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional
    public void runDailyJobs() {
        log.info("=== 🌙 Starting Nightly Jobs (12:05 AM) ===");

        // STEP 1: Mark old unpaid bills as OVERDUE
        // (This checks if any Due Date passed yesterday)
        log.info(">> Step 1: Updating overdue statuses...");
        installmentService.updateOverdueStatuses();

        // STEP 2: Create NEW installments for the next month
        // (Checks if today is the player's 'Join Anniversary')
        log.info(">> Step 2: Generating new monthly installments...");
        generateDailyInstallments();

        log.info("=== ✅ Nightly Jobs Completed ===");
    }

    /**
     * Logic:
     * 1. Find players whose "Join Day" is today (or earlier in the month if we missed them).
     * 2. Check if they already have a bill for THIS month.
     * 3. If not, create it.
     */
    private void generateDailyInstallments() {
        LocalDate today = LocalDate.now();
        int currentDay = today.getDayOfMonth();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();

        // Use 'LessThanEqual' to be safe (Catch-up mode).
        // It ensures even if server was off yesterday, we generate the bill today.
        List<Player> players = playerRepository.findByJoinDayLessThanEqual(currentDay);

        for (Player p : players) {
            // Check if bill exists for THIS month to avoid duplicates
            boolean exists = installmentRepository.existsByPlayerIdAndPeriodMonthAndPeriodYear(
                    p.getId(), currentMonth, currentYear
            );

            if (!exists) {
                // Create logic:
                // Period: Current Month (e.g., Jan)
                // Due Date: 1 Month from Today (e.g., Feb 1st)
                LocalDate dueDate = today.plusMonths(1);

                try {
                    installmentService.createInstallmentForPlayer(
                            p.getId(), currentMonth, currentYear, dueDate, null
                    );
                    log.info("Generated installment for {}", p.getName());
                } catch (Exception e) {
                    log.error("Error generating for {}: {}", p.getName(), e.getMessage());
                }
            }
        }
    }
}