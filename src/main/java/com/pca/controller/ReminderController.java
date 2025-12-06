package com.pca.controller;

import com.pca.dto.ReminderRequestDTO;
import com.pca.dto.ReminderResponseDTO;
import com.pca.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
@Slf4j
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping
    public ReminderResponseDTO sendReminder(@Valid @RequestBody ReminderRequestDTO request) {
        return reminderService.createReminderLog(request);
    }

    @GetMapping("/installment/{installmentId}")
    public List<ReminderResponseDTO> getByInstallment(@PathVariable Long installmentId) {
        return reminderService.getReminderHistory(installmentId);
    }
}
