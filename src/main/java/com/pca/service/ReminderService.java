package com.pca.service;

import com.pca.dto.ReminderRequestDTO;
import com.pca.dto.ReminderResponseDTO;
import com.pca.exception.ResourceNotFoundException;
import com.pca.model.Installment;
import com.pca.model.ReminderHistory;
import com.pca.repository.ReminderHistoryRepository;
import com.pca.repository.InstallmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {

    private final ReminderHistoryRepository reminderHistoryRepository;
    private final InstallmentRepository installmentRepository;

    @Transactional
    public ReminderResponseDTO createReminderLog(ReminderRequestDTO req) {
        log.info("Creating reminder log for installment {}", req.getInstallmentId());
        Installment inst = installmentRepository.findById(req.getInstallmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found: " + req.getInstallmentId()));

        ReminderHistory rh = ReminderHistory.builder()
                .installment(inst)
                .sentAt(LocalDateTime.now())
                .type(ReminderHistory.ReminderType.valueOf(req.getType()))
                .triggeredBy(req.getTriggeredBy() == null ? "COACH" : req.getTriggeredBy())
                .snoozeDate(req.getSnoozeDate())
                .build();

        ReminderHistory saved = reminderHistoryRepository.save(rh);
        return toDto(saved);
    }

    public List<ReminderResponseDTO> getReminderHistory(Long installmentId) {
        return reminderHistoryRepository.findByInstallmentId(installmentId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private ReminderResponseDTO toDto(ReminderHistory r) {
        return ReminderResponseDTO.builder()
                .id(r.getId())
                .installmentId(r.getInstallment() != null ? r.getInstallment().getId() : null)
                .sentAt(r.getSentAt())
                .type(r.getType() == null ? null : r.getType().name())
                .triggeredBy(r.getTriggeredBy())
                .snoozeDate(r.getSnoozeDate())
                .build();
    }
}
