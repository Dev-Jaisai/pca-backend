package com.pca.service;

import com.pca.dto.TotalAmountDTO;
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
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class TotalAmountService {

    private final InstallmentRepository installmentRepository;
    private final PlayerRepository playerRepository;

    public TotalAmountDTO calculateTotalAmount(Long playerId) {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        // Get installments up to current month
        List<Installment> installments = installmentRepository
                .findInstallmentsUpToCurrentMonth(playerId, currentYear, currentMonth);

        if (installments.isEmpty()) {
            return TotalAmountDTO.builder()
                    .playerId(playerId)
                    .playerName(playerRepository.findById(playerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Player not found"))
                            .getName())
                    .totalAmount(BigDecimal.ZERO)
                    .totalPaid(BigDecimal.ZERO)
                    .totalRemaining(BigDecimal.ZERO)
                    .installmentCount(0)
                    .periodRange("No installments")
                    .build();
        }

        // Calculate totals
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;

        for (Installment inst : installments) {
            totalAmount = totalAmount.add(BigDecimal.valueOf(inst.getAmount()));
            totalPaid = totalPaid.add(BigDecimal.valueOf(inst.getPaidAmount()));
            totalRemaining = totalRemaining.add(BigDecimal.valueOf(inst.getRemainingAmount()));
        }

        // Get period range
        String periodRange = getPeriodRange(installments);

        return TotalAmountDTO.builder()
                .playerId(playerId)
                .playerName(installments.get(0).getPlayer().getName())
                .totalAmount(totalAmount)
                .totalPaid(totalPaid)
                .totalRemaining(totalRemaining)
                .installmentCount(installments.size())
                .periodRange(periodRange)
                .build();
    }

    private String getPeriodRange(List<Installment> installments) {
        if (installments.isEmpty()) return "";

        // Find earliest and latest installments
        Installment earliest = installments.get(0);
        Installment latest = installments.get(0);

        for (Installment inst : installments) {
            YearMonth currentYM = YearMonth.of(inst.getPeriodYear(), inst.getPeriodMonth());
            YearMonth earliestYM = YearMonth.of(earliest.getPeriodYear(), earliest.getPeriodMonth());
            YearMonth latestYM = YearMonth.of(latest.getPeriodYear(), latest.getPeriodMonth());

            if (currentYM.isBefore(earliestYM)) {
                earliest = inst;
            }
            if (currentYM.isAfter(latestYM)) {
                latest = inst;
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
        String start = YearMonth.of(earliest.getPeriodYear(), earliest.getPeriodMonth())
                .format(formatter);
        String end = YearMonth.of(latest.getPeriodYear(), latest.getPeriodMonth())
                .format(formatter);

        return start + " - " + end;
    }

    // Get totals for ALL players (for dashboard)
    public BigDecimal getGrandTotalAmount() {
        Double total = installmentRepository.findTotalAmountForAllPlayers();
        return total != null ? BigDecimal.valueOf(total) : BigDecimal.ZERO;
    }

    public BigDecimal getGrandTotalPaid() {
        Double paid = installmentRepository.findTotalPaidForAllPlayers();
        return paid != null ? BigDecimal.valueOf(paid) : BigDecimal.ZERO;
    }

    public BigDecimal getGrandTotalRemaining() {
        Double remaining = installmentRepository.findTotalRemainingForAllPlayers();
        return remaining != null ? BigDecimal.valueOf(remaining) : BigDecimal.ZERO;
    }
}