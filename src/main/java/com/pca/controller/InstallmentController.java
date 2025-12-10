package com.pca.controller;

import com.pca.dto.InstallmentRequestDTO;
import com.pca.dto.InstallmentResponseDTO;
import com.pca.dto.LatestInstallmentMonthDTO;
import com.pca.dto.PlayerInstallmentSummaryDTO;
import com.pca.service.InstallmentService;
import com.pca.service.PlayerInstallmentSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/installments")
@RequiredArgsConstructor
@Slf4j
public class InstallmentController {

    private final InstallmentService installmentService;
    private final PlayerInstallmentSummaryService summaryService;

    @PostMapping("/generate-monthly")
    public String generateMonthly(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam String dueDate
    ) {
        installmentService.generateForAllPlayers(month, year, dueDate);
        return "Installments generated successfully";
    }

    @PostMapping
    public InstallmentResponseDTO create(@Valid @RequestBody InstallmentRequestDTO request) {
        return installmentService.createInstallment(request);
    }

    @GetMapping("/player/{playerId}")
    public List<InstallmentResponseDTO> getByPlayer(@PathVariable Long playerId) {
        return installmentService.getInstallmentsByPlayer(playerId);
    }

    /**
     * NEW ENDPOINT: Get ALL installments for ALL players
     */
    @GetMapping("/all-summary")
    public ResponseEntity<List<PlayerInstallmentSummaryDTO>> getAllSummary() {
        List<PlayerInstallmentSummaryDTO> list = summaryService.getAllInstallmentsSummary();
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/installments/summary?month=YYYY-MM (month-specific)
     * Uses the original method name: summaryService.getSummary()
     */
    @GetMapping("/summary")
    public ResponseEntity<List<PlayerInstallmentSummaryDTO>> summary(@RequestParam("month") String month,
                                                                     @RequestParam(name = "filter", required = false) String filter) {
        List<PlayerInstallmentSummaryDTO> list = summaryService.getSummary(month);
        if (filter != null && !filter.isBlank()) {
            String f = filter.trim().toLowerCase();
            list.removeIf(dto -> !dto.getStatus().toLowerCase().equals(f));
        }
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/installments/latest-month
     * Returns the most recent year/month for which installments exist,
     * or current month if none found.
     */
    @GetMapping("/latest-month")
    public ResponseEntity<LatestInstallmentMonthDTO> latestMonth() {
        LatestInstallmentMonthDTO dto = installmentService.getLatestInstallmentMonth();
        return ResponseEntity.ok(dto);
    }
    // ... inside InstallmentController class ...

    @PostMapping("/refresh-overdue")
    public ResponseEntity<String> refreshOverdue() {
        installmentService.updateOverdueStatuses();
        return ResponseEntity.ok("Overdue statuses updated successfully");
    }
}