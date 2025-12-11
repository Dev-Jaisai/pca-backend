package com.pca.service;

import com.pca.dto.OverdueDetailsDTO;
import com.pca.dto.OverdueMonthDTO;
import com.pca.exception.ResourceNotFoundException;
import com.pca.model.Installment;
import com.pca.repository.InstallmentRepository;
import com.pca.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OverdueCalculationService {

    private final InstallmentRepository installmentRepository;
    private final PlayerRepository playerRepository;

    public OverdueDetailsDTO getOverdueDetails(Long playerId) {
        LocalDate today = LocalDate.now();

        // Get all overdue installments
        List<Installment> overdueInstallments = installmentRepository
                .findOverdueInstallmentsByPlayer(playerId, today);

        if (overdueInstallments.isEmpty()) {
            return createEmptyResponse(playerId);
        }

        // Calculate totals
        BigDecimal totalOverdueAmount = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;

        List<OverdueMonthDTO> overdueMonths = new ArrayList<>();
        YearMonth firstOverdueMonth = null;
        YearMonth latestOverdueMonth = null;

        for (Installment inst : overdueInstallments) {
            YearMonth currentYM = YearMonth.of(inst.getPeriodYear(), inst.getPeriodMonth());

            // Track first and latest overdue months
            if (firstOverdueMonth == null || currentYM.isBefore(firstOverdueMonth)) {
                firstOverdueMonth = currentYM;
            }
            if (latestOverdueMonth == null || currentYM.isAfter(latestOverdueMonth)) {
                latestOverdueMonth = currentYM;
            }

            // Accumulate totals
            totalOverdueAmount = totalOverdueAmount.add(BigDecimal.valueOf(inst.getAmount()));
            totalPaid = totalPaid.add(BigDecimal.valueOf(inst.getPaidAmount()));
            totalRemaining = totalRemaining.add(BigDecimal.valueOf(inst.getRemainingAmount()));

            // Create month detail
            OverdueMonthDTO monthDetail = OverdueMonthDTO.builder()
                    .yearMonth(currentYM)
                    .monthName(formatMonthYear(inst.getPeriodYear(), inst.getPeriodMonth()))
                    .amount(BigDecimal.valueOf(inst.getAmount()))
                    .paidAmount(BigDecimal.valueOf(inst.getPaidAmount()))
                    .remainingAmount(BigDecimal.valueOf(inst.getRemainingAmount()))
                    .dueDate(inst.getDueDate())
                    .status(inst.getStatus() != null ? inst.getStatus().name() : "OVERDUE")
                    .installmentId(inst.getId())
                    .build();

            overdueMonths.add(monthDetail);
        }

        String playerName = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"))
                .getName();

        return OverdueDetailsDTO.builder()
                .playerId(playerId)
                .playerName(playerName)
                .overdueMonthCount(overdueInstallments.size())
                .totalOverdueAmount(totalOverdueAmount)
                .totalPaid(totalPaid)
                .totalRemaining(totalRemaining)
                .overdueMonths(overdueMonths)
                .firstOverdueMonth(firstOverdueMonth)
                .latestOverdueMonth(latestOverdueMonth)
                .build();
    }
    public List<Map<String, Object>> getAllOverduePlayersSummary() {
        LocalDate today = LocalDate.now();

        // First, add this method to InstallmentRepository:
        // @Query("SELECT DISTINCT i.player.id FROM Installment i WHERE i.dueDate < CURRENT_DATE AND i.remainingAmount > 0")
        // List<Long> findPlayersWithOverdue();

        List<Long> playerIds = installmentRepository.findPlayersWithOverdue(today);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Long playerId : playerIds) {
            OverdueDetailsDTO details = getOverdueDetails(playerId);

            Map<String, Object> playerSummary = new HashMap<>();
            playerSummary.put("playerId", details.getPlayerId());
            playerSummary.put("playerName", details.getPlayerName());
            playerSummary.put("overdueMonthCount", details.getOverdueMonthCount());
            playerSummary.put("totalOverdueAmount", details.getTotalOverdueAmount());
            playerSummary.put("totalRemaining", details.getTotalRemaining());
            playerSummary.put("firstOverdueDate", details.getFirstOverdueMonth() != null ?
                    details.getFirstOverdueMonth().toString() : "N/A");

            result.add(playerSummary);
        }

        return result;
    }
    // Get only count of overdue months (lightweight)
    public Integer getOverdueMonthCount(Long playerId) {
        LocalDate today = LocalDate.now();
        List<Installment> overdueInstallments = installmentRepository
                .findOverdueInstallmentsByPlayer(playerId, today);
        return overdueInstallments.size();
    }

    // Get overdue summary for dashboard
    public Map<String, Object> getOverdueSummary(Long playerId) {
        OverdueDetailsDTO details = getOverdueDetails(playerId);

        return Map.of(
                "playerId", details.getPlayerId(),
                "playerName", details.getPlayerName(),
                "overdueMonthCount", details.getOverdueMonthCount(),
                "totalOverdueAmount", details.getTotalOverdueAmount(),
                "totalRemaining", details.getTotalRemaining(),
                "firstOverdueMonth", details.getFirstOverdueMonth() != null ?
                        details.getFirstOverdueMonth().toString() : "N/A",
                "latestOverdueMonth", details.getLatestOverdueMonth() != null ?
                        details.getLatestOverdueMonth().toString() : "N/A"
        );
    }

    private OverdueDetailsDTO createEmptyResponse(Long playerId) {
        String playerName = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"))
                .getName();

        return OverdueDetailsDTO.builder()
                .playerId(playerId)
                .playerName(playerName)
                .overdueMonthCount(0)
                .totalOverdueAmount(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .totalRemaining(BigDecimal.ZERO)
                .overdueMonths(List.of())
                .firstOverdueMonth(null)
                .latestOverdueMonth(null)
                .build();
    }

    private String formatMonthYear(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        return ym.format(formatter);
    }
}