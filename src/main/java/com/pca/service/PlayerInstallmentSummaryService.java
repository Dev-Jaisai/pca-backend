package com.pca.service;

import com.pca.dto.PlayerInstallmentSummaryDTO;
import com.pca.repository.PlayerInstallmentSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerInstallmentSummaryService {

    private final PlayerInstallmentSummaryRepository repository;

    public List<PlayerInstallmentSummaryDTO> getSummary(String monthParam) {
        if (monthParam == null || !monthParam.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("Invalid month format. Expected YYYY-MM");
        }
        int year = Integer.parseInt(monthParam.substring(0, 4));
        int month = Integer.parseInt(monthParam.substring(5, 7));

        List<Object[]> rows = repository.fetchSummary(year, month);
        return rows.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PlayerInstallmentSummaryDTO> getAllInstallmentsSummary() {
        List<Object[]> rows = repository.fetchAllSummary();
        return rows.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PlayerInstallmentSummaryDTO> getSummaryForPlayer(Long playerId) {
        return getAllInstallmentsSummary().stream()
                .filter(dto -> dto.getPlayerId().equals(playerId))
                .collect(Collectors.toList());
    }

    public List<PlayerInstallmentSummaryDTO> getSummaryByStatus(String status) {
        if (status == null) return new ArrayList<>();
        return getAllInstallmentsSummary().stream()
                .filter(dto -> status.equalsIgnoreCase(dto.getStatus()))
                .collect(Collectors.toList());
    }

    // 🔥 FIX 1: Completely removed BigDecimal usage here
    private PlayerInstallmentSummaryDTO mapToDto(Object[] row) {
        try {
            Long playerId = ((Number) row[0]).longValue();
            String playerName = (String) row[1];
            String phone = (String) row[2];
            String groupName = (String) row[3];

            LocalDate joinDate = null;
            if (row[4] != null) {
                if (row[4] instanceof java.sql.Date) joinDate = ((java.sql.Date) row[4]).toLocalDate();
                else if (row[4] instanceof java.sql.Timestamp) joinDate = ((java.sql.Timestamp) row[4]).toLocalDateTime().toLocalDate();
            }

            Long installmentId = row[5] != null ? ((Number) row[5]).longValue() : null;

            // ✅ Corrected: Directly cast to Double using Number
            Double amount = row[6] != null ? ((Number) row[6]).doubleValue() : null;
            Double totalPaid = row[7] != null ? ((Number) row[7]).doubleValue() : 0.0;

            LocalDate dueDate = null;
            if (row[8] != null) {
                if (row[8] instanceof java.sql.Date) dueDate = ((java.sql.Date) row[8]).toLocalDate();
                else if (row[8] instanceof java.sql.Timestamp) dueDate = ((java.sql.Timestamp) row[8]).toLocalDateTime().toLocalDate();
            }

            String status = (String) row[9];

            // ✅ Corrected
            Double remaining = row[10] != null ? ((Number) row[10]).doubleValue() : null;

            LocalDateTime lastPaymentDate = null;
            if (row.length > 11 && row[11] != null) {
                if (row[11] instanceof java.sql.Timestamp) {
                    lastPaymentDate = ((java.sql.Timestamp) row[11]).toLocalDateTime();
                } else if (row[11] instanceof LocalDateTime) {
                    lastPaymentDate = (LocalDateTime) row[11];
                }
            }

            String notes = null;
            if (row.length > 12 && row[12] != null) {
                notes = (String) row[12];
            }

            if (installmentId != null && status == null) {
                status = "PENDING";
            } else if (installmentId == null) {
                status = "NO_INSTALLMENT";
            }

            return PlayerInstallmentSummaryDTO.builder()
                    .playerId(playerId)
                    .playerName(playerName)
                    .phone(phone)
                    .groupName(groupName)
                    .joinDate(joinDate)
                    .installmentId(installmentId)
                    .installmentAmount(amount)
                    .totalPaid(totalPaid)
                    .remaining(remaining)
                    .dueDate(dueDate)
                    .status(status)
                    .lastPaymentDate(lastPaymentDate)
                    .notes(notes)
                    .build();
        } catch (Exception e) {
            log.error("Error mapping row for player: " + (row.length > 1 ? row[1] : "Unknown"), e);
            return null;
        }
    }

    public Page<PlayerInstallmentSummaryDTO> getAllInstallmentsSummary(Pageable pageable) {
        List<Object[]> rows = repository.fetchAllSummaryPaginated(
                pageable.getPageSize(),
                pageable.getOffset()
        );

        long total = repository.countAllInstallments();

        List<PlayerInstallmentSummaryDTO> dtos = rows.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, total);
    }

    public List<PlayerInstallmentSummaryDTO> getOverdueSummary() {
        List<Object[]> rows = repository.fetchOverdueSummary();
        return rows.stream()
                .map(this::mapOverdueToDto)
                .collect(Collectors.toList());
    }

    // 🔥 FIX 2: Completely removed BigDecimal usage here too
    private PlayerInstallmentSummaryDTO mapOverdueToDto(Object[] row) {
        try {
            Long playerId = ((Number) row[0]).longValue();
            String playerName = (String) row[1];
            String phone = (String) row[2];
            String groupName = (String) row[3];

            LocalDate joinDate = null;
            if (row[4] != null) {
                if (row[4] instanceof java.sql.Date) joinDate = ((java.sql.Date) row[4]).toLocalDate();
                else if (row[4] instanceof java.sql.Timestamp) joinDate = ((java.sql.Timestamp) row[4]).toLocalDateTime().toLocalDate();
            }

            // ✅ Corrected: Use ((Number) val).doubleValue()
            Double totalInstallmentAmount = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
            Double totalPaid = row[7] != null ? ((Number) row[7]).doubleValue() : 0.0;

            LocalDate latestDueDate = null;
            if (row[8] != null) {
                if (row[8] instanceof java.sql.Date) latestDueDate = ((java.sql.Date) row[8]).toLocalDate();
                else if (row[8] instanceof java.sql.Timestamp) latestDueDate = ((java.sql.Timestamp) row[8]).toLocalDateTime().toLocalDate();
            }

            String statuses = (String) row[9];

            // ✅ Corrected
            Double totalRemaining = row[10] != null ? ((Number) row[10]).doubleValue() : 0.0;

            LocalDateTime lastPaymentDate = null;
            if (row.length > 11 && row[11] != null) {
                if (row[11] instanceof java.sql.Timestamp) {
                    lastPaymentDate = ((java.sql.Timestamp) row[11]).toLocalDateTime();
                } else if (row[11] instanceof LocalDateTime) {
                    lastPaymentDate = (LocalDateTime) row[11];
                }
            }

            String overallStatus = "PENDING";
            if (totalRemaining == 0) {
                overallStatus = "PAID";
            } else if (statuses != null && statuses.contains("OVERDUE")) {
                overallStatus = "OVERDUE";
            }

            return PlayerInstallmentSummaryDTO.builder()
                    .playerId(playerId)
                    .playerName(playerName)
                    .phone(phone)
                    .groupName(groupName)
                    .joinDate(joinDate)
                    .installmentAmount(totalInstallmentAmount)
                    .totalPaid(totalPaid)
                    .remaining(totalRemaining)
                    .dueDate(latestDueDate)
                    .status(overallStatus)
                    .lastPaymentDate(lastPaymentDate)
                    .notes(null)
                    .build();
        } catch (Exception e) {
            log.error("Error mapping overdue row for player: " + (row.length > 1 ? row[1] : "Unknown"), e);
            return null;
        }
    }
}