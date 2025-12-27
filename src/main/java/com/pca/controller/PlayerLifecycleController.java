// PlayerLifecycleController.java

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

    // 🔥 UPDATED: Accept Advance Amount
    @PostMapping("/{id}/pause")
    public ResponseEntity<String> pausePlayer(
            @PathVariable Long id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("reason") String reason,
            @RequestParam(value = "advanceAmount", required = false) Double advanceAmount) { // 🔥 NEW

        lifecycleService.pausePlayer(id, date, reason, advanceAmount); // Pass to service
        return ResponseEntity.ok("Player paused successfully");
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<String> activatePlayer(
            @PathVariable Long id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        lifecycleService.activatePlayer(id, date);
        return ResponseEntity.ok("Player activated successfully");
    }

    @PostMapping("/{id}/left")
    public ResponseEntity<String> markPlayerLeft(
            @PathVariable Long id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("option") com.pca.enums.LeftOption option,
            @RequestParam(value = "amount", required = false) Double amount) {

        lifecycleService.markPlayerLeft(id, date, option, amount);
        return ResponseEntity.ok("Player marked as LEFT successfully");
    }
}