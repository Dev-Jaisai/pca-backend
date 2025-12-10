package com.pca.service;

import com.pca.dto.PlayerInstallmentSummaryDTO;
import com.pca.repository.InstallmentRepository;
import com.pca.repository.proj.PlayerInstallmentSummaryProjection;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InstallmentSummaryService {

    private final InstallmentRepository installmentRepository;

    public InstallmentSummaryService(InstallmentRepository installmentRepository) {
        this.installmentRepository = installmentRepository;
    }

    public List<PlayerInstallmentSummaryDTO> getSummaryForMonth(String month) {

        // Validate format: YYYY-MM
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("Invalid month format. Expected YYYY-MM (example: 2025-12)");
        }

        int year = Integer.parseInt(month.substring(0, 4));
        int periodMonth = Integer.parseInt(month.substring(5, 7));

        // Extra rule: month must be 1..12
        if (periodMonth < 1 || periodMonth > 12) {
            throw new IllegalArgumentException("Month value must be between 01 and 12");
        }

        List<PlayerInstallmentSummaryProjection> rows =
                installmentRepository.findSummaryByPeriod(periodMonth, year);

        return rows.stream().map(r -> {
            BigDecimal installmentAmount = r.getInstallmentAmount() != null ? r.getInstallmentAmount() : BigDecimal.ZERO;
            BigDecimal totalPaid = r.getTotalPaid() != null ? r.getTotalPaid() : BigDecimal.ZERO;
            BigDecimal remaining = installmentAmount.subtract(totalPaid);

            String status;
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                status = "Paid";
            } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                status = "Partially Paid";
            } else {
                LocalDate due = r.getDueDate();
                status = (due != null && due.isBefore(LocalDate.now())) ? "Overdue" : "Pending";
            }

            return PlayerInstallmentSummaryDTO.builder()
                    .playerId(r.getPlayerId())
                    .playerName(r.getPlayerName())
                    .phone(r.getPhone())
                    .groupName(r.getGroupName())
                    .joinDate(r.getJoinDate())
                    .installmentAmount(installmentAmount)
                    .totalPaid(totalPaid)
                    .remaining(remaining)
                    .dueDate(r.getDueDate())
                    .status(status)
                    .installmentId(r.getInstallmentId())
                    .build();
        }).collect(Collectors.toList());
    }

}
