package com.pca.controller;

import com.pca.service.PlayerLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/player-lifecycle")
@RequiredArgsConstructor
public class PlayerLifecycleController {

    private final PlayerLifecycleService lifecycleService;

    // API: Pause (Holiday)
    // URL: POST /api/player-lifecycle/1/pause?date=2025-02-01&reason=Village
    @PostMapping("/{id}/pause")
    public ResponseEntity<String> pausePlayer(
            @PathVariable Long id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("reason") String reason) {

        lifecycleService.pausePlayer(id, date, reason);
        return ResponseEntity.ok("Player paused successfully");
    }

    // API: Activate (Return)
    // URL: POST /api/player-lifecycle/1/activate?date=2025-04-01
    @PostMapping("/{id}/activate")
    public ResponseEntity<String> activatePlayer(
            @PathVariable Long id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        lifecycleService.activatePlayer(id, date);
        return ResponseEntity.ok("Player activated successfully");
    }
}