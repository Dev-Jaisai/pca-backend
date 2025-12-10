package com.pca.service;

import com.pca.dto.PlayerInstallmentSummaryDTO;
import com.pca.repository.PlayerInstallmentSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    /**
     * Get summary for a specific month (e.g. "2025-12").
     * Uses the native query with year/month filter.
     */
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

    /**
     * Get ALL installments (Past, Present, Future).
     * Uses the native query with NO filter.
     */
    public List<PlayerInstallmentSummaryDTO> getAllInstallmentsSummary() {
        List<Object[]> rows = repository.fetchAllSummary();
        return rows.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * FIX: Added missing method.
     * Reuses the main list and filters by Player ID.
     */
    public List<PlayerInstallmentSummaryDTO> getSummaryForPlayer(Long playerId) {
        return getAllInstallmentsSummary().stream()
                .filter(dto -> dto.getPlayerId().equals(playerId))
                .collect(Collectors.toList());
    }

    /**
     * FIX: Added missing method.
     * Reuses the main list and filters by Status.
     */
    public List<PlayerInstallmentSummaryDTO> getSummaryByStatus(String status) {
        if (status == null) return new ArrayList<>();
        return getAllInstallmentsSummary().stream()
                .filter(dto -> status.equalsIgnoreCase(dto.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Maps the raw SQL result (Object[]) to the DTO.
     * 11: MAX(pay.paid_on) <-- Includes Last Payment Date
     */
    private PlayerInstallmentSummaryDTO mapToDto(Object[] row) {
        try {
            Long playerId = ((Number) row[0]).longValue();
            String playerName = (String) row[1];
            String phone = (String) row[2];
            String groupName = (String) row[3];

            // Handle SQL Date vs LocalDate
            LocalDate joinDate = null;
            if (row[4] != null) {
                if (row[4] instanceof java.sql.Date) joinDate = ((java.sql.Date) row[4]).toLocalDate();
                else if (row[4] instanceof java.sql.Timestamp) joinDate = ((java.sql.Timestamp) row[4]).toLocalDateTime().toLocalDate();
            }

            Long installmentId = row[5] != null ? ((Number) row[5]).longValue() : null;
            BigDecimal amount = row[6] != null ? BigDecimal.valueOf(((Number) row[6]).doubleValue()) : null;
            BigDecimal totalPaid = row[7] != null ? BigDecimal.valueOf(((Number) row[7]).doubleValue()) : BigDecimal.ZERO;

            LocalDate dueDate = null;
            if (row[8] != null) {
                if (row[8] instanceof java.sql.Date) dueDate = ((java.sql.Date) row[8]).toLocalDate();
                else if (row[8] instanceof java.sql.Timestamp) dueDate = ((java.sql.Timestamp) row[8]).toLocalDateTime().toLocalDate();
            }

            String status = (String) row[9];
            BigDecimal remaining = row[10] != null ? BigDecimal.valueOf(((Number) row[10]).doubleValue()) : null;

            // --- Map Last Payment Date ---
            LocalDateTime lastPaymentDate = null;
            if (row.length > 11 && row[11] != null) {
                if (row[11] instanceof java.sql.Timestamp) {
                    lastPaymentDate = ((java.sql.Timestamp) row[11]).toLocalDateTime();
                } else if (row[11] instanceof LocalDateTime) {
                    lastPaymentDate = (LocalDateTime) row[11];
                }
            }

            // Fallback status logic
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
                    .lastPaymentDate(lastPaymentDate) // <--- Correctly populated
                    .build();
        } catch (Exception e) {
            log.error("Error mapping row for player: " + (row.length > 1 ? row[1] : "Unknown"), e);
            return null; // or throw
        }
    }

    // ... inside class ...

    public Page<PlayerInstallmentSummaryDTO> getAllInstallmentsSummary(Pageable pageable) {
        // Fetch rows with limit/offset
        List<Object[]> rows = repository.fetchAllSummaryPaginated(
                pageable.getPageSize(),
                pageable.getOffset()
        );

        // Get total count
        long total = repository.countAllInstallments();

        // Convert to DTOs
        List<PlayerInstallmentSummaryDTO> dtos = rows.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, total);
    }
}