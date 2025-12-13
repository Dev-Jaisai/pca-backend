package com.pca.controller;

import com.pca.dto.TotalAmountDTO;
import com.pca.service.TotalAmountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/totals")
@RequiredArgsConstructor
@Slf4j
public class TotalAmountController {

    private final TotalAmountService totalAmountService;



    @GetMapping("/player/{playerId}")
    public ResponseEntity<TotalAmountDTO> getPlayerTotal(@PathVariable Long playerId) {
        log.info("Getting total amounts for player: {}", playerId);
        TotalAmountDTO result = totalAmountService.calculateTotalAmount(playerId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/grand-total")
    public ResponseEntity<Map<String, BigDecimal>> getGrandTotal() {
        log.info("Getting grand totals for all players");
        Map<String, BigDecimal> totals = Map.of(
                "totalAmount", totalAmountService.getGrandTotalAmount(),
                "totalPaid", totalAmountService.getGrandTotalPaid(),
                "totalRemaining", totalAmountService.getGrandTotalRemaining()
        );
        return ResponseEntity.ok(totals);
    }

    @GetMapping("/player/{playerId}/up-to-current")
    public ResponseEntity<TotalAmountDTO> getUpToCurrentMonth(@PathVariable Long playerId) {
        log.info("Getting totals up to current month for player: {}", playerId);
        TotalAmountDTO result = totalAmountService.calculateTotalAmount(playerId);
        return ResponseEntity.ok(result);
    }
}