// com.pca.controller.InstallmentStatusController.java
package com.pca.controller;

import com.pca.dto.InstallmentStatusDTO;
import com.pca.service.InstallmentStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@Slf4j
public class InstallmentStatusController {

    private final InstallmentStatusService installmentStatusService;

    /**
     * Example:
     * GET /api/players/installment-status
     * GET /api/players/installment-status?month=3&year=2025
     *
     * Returns a list of { playerId, hasInstallments } for ALL players.
     */
    @GetMapping("/installment-status")
    public List<InstallmentStatusDTO> getInstallmentStatus(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        log.debug("Fetching installment status month={} year={}", month, year);
        return installmentStatusService.getInstallmentStatusForAllPlayers(month, year);
    }


}
