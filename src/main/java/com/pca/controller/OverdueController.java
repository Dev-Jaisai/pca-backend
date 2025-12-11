package com.pca.controller;

import com.pca.dto.OverdueDetailsDTO;
import com.pca.service.OverdueCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/overdue")
@RequiredArgsConstructor
@Slf4j
public class OverdueController {

    private final OverdueCalculationService overdueCalculationService;

    @GetMapping("/player/{playerId}/details")
    public ResponseEntity<OverdueDetailsDTO> getOverdueDetails(@PathVariable Long playerId) {
        log.info("Getting overdue details for player: {}", playerId);
        OverdueDetailsDTO result = overdueCalculationService.getOverdueDetails(playerId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/player/{playerId}/count")
    public ResponseEntity<Map<String, Integer>> getOverdueMonthCount(@PathVariable Long playerId) {
        log.info("Getting overdue month count for player: {}", playerId);
        Integer count = overdueCalculationService.getOverdueMonthCount(playerId);
        return ResponseEntity.ok(Map.of("overdueMonthCount", count));
    }

    @GetMapping("/player/{playerId}/summary")
    public ResponseEntity<Map<String, Object>> getOverdueSummary(@PathVariable Long playerId) {
        log.info("Getting overdue summary for player: {}", playerId);
        Map<String, Object> result = overdueCalculationService.getOverdueSummary(playerId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllOverduePlayers() {
        log.info("Getting all players with overdue installments");
        List<Map<String, Object>> result = overdueCalculationService.getAllOverduePlayersSummary();
        return ResponseEntity.ok(result);
    }
}