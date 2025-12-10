package com.pca.scheduler;

import com.pca.service.InstallmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueScheduler {

    private final InstallmentService installmentService;

    // This runs automatically every day at 00:00:00 (Midnight)
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkOverdueInstallments() {
        log.info("Daily Scheduler: Checking for overdue installments...");
        installmentService.updateOverdueStatuses();
    }
}