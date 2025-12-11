package com.pca.controller;

import com.pca.dto.PlayerInstallmentSummaryDTO;
import com.pca.service.PlayerInstallmentSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@Slf4j
public class PlayerInstallmentSummaryController {

    private final PlayerInstallmentSummaryService summaryService;

    /**
     * GET /api/players/installment-summary?month=YYYY-MM
     * Returns installment summary for all players for a specific month
     */
    @GetMapping("/installment-summary")
    public ResponseEntity<List<PlayerInstallmentSummaryDTO>> getSummaryForMonth(
            @RequestParam("month") String month) {
        log.info("Fetching installment summary for month: {}", month);
        List<PlayerInstallmentSummaryDTO> list = summaryService.getSummary(month);
        return ResponseEntity.ok(list);
    }

/*
    */
/**
     * GET /api/players/installment-summary/all
     * Returns ALL installments for ALL players (not month-filtered)
     *//*

    @GetMapping("/installment-summary/all")
    public ResponseEntity<List<PlayerInstallmentSummaryDTO>> getAllSummary() {
        log.info("Fetching ALL installment summary");
        List<PlayerInstallmentSummaryDTO> list = summaryService.getAllInstallmentsSummary();
        return ResponseEntity.ok(list);
    }
*/

    /**
     * GET /api/players/installment-summary/player/{playerId}
     * Returns installment summary for a specific player
     */
    @GetMapping("/installment-summary/player/{playerId}")
    public ResponseEntity<List<PlayerInstallmentSummaryDTO>> getSummaryForPlayer(
            @PathVariable Long playerId) {
        log.info("Fetching installment summary for player: {}", playerId);
        // You'll need to add a method in the service to get summary for a specific player
        List<PlayerInstallmentSummaryDTO> list = summaryService.getSummaryForPlayer(playerId);
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/players/installment-summary/status/{status}
     * Returns players with a specific installment status
     */
    @GetMapping("/installment-summary/status/{status}")
    public ResponseEntity<List<PlayerInstallmentSummaryDTO>> getSummaryByStatus(
            @PathVariable String status) {
        log.info("Fetching installment summary with status: {}", status);
        List<PlayerInstallmentSummaryDTO> list = summaryService.getSummaryByStatus(status);
        return ResponseEntity.ok(list);
    }
    @GetMapping("/overdue-summary")
    public ResponseEntity<List<PlayerInstallmentSummaryDTO>> getOverdueSummary() {
        log.info("Fetching overdue summary (cumulative amounts)");
        List<PlayerInstallmentSummaryDTO> list = summaryService.getOverdueSummary();
        return ResponseEntity.ok(list);
    }
}