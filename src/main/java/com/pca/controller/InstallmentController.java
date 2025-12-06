package com.pca.controller;

import com.pca.dto.InstallmentRequestDTO;
import com.pca.dto.InstallmentResponseDTO;
import com.pca.service.InstallmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/installments")
@RequiredArgsConstructor
@Slf4j
public class InstallmentController {

    private final InstallmentService installmentService;

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
}
