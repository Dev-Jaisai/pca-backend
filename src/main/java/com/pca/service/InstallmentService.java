package com.pca.service;

import com.pca.dto.*;
import com.pca.exception.ResourceNotFoundException;
import com.pca.model.*;
import com.pca.repository.InstallmentRepository;
import com.pca.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstallmentService {

    private final InstallmentRepository installmentRepository;
    private final PlayerRepository playerRepository;
    private final FeeStructureService feeStructureService;

    /**
     * Called by controller to generate monthly installments for all players.
     * dueDateParam should be ISO date string (yyyy-MM-dd).
     */
    // 2. AUTO / BULK GENERATION
    @Transactional
    public void generateForAllPlayers(int month, int year, String dueDateParam) {
        log.info("Generating monthly installments for {}/{}", month, year);
        LocalDate dueDate;
        try {
            dueDate = LocalDate.parse(dueDateParam);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid dueDate format. Use yyyy-MM-dd");
        }

        List<Player> players = playerRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Player p : players) {
            if (installmentRepository.existsByPlayerIdAndPeriodMonthAndPeriodYear(p.getId(), month, year)) {
                log.debug("Installment already exists for player {} month {} year {}", p.getId(), month, year);
                continue;
            }

            // 🔥🔥🔥 FIX: Check if Player is Inactive (Holiday) 🔥🔥🔥
            if (!p.getIsActive()) {
                Installment holidayBill = Installment.builder()
                        .player(p)
                        .periodMonth(month)
                        .periodYear(year)
                        .amount(0.0)
                        .paidAmount(0.0)
                        .remainingAmount(0.0)
                        .status(Installment.Status.SKIPPED) // Cyan Chip
                        .notes("Pre-informed Holiday (Auto-Gen)")
                        .dueDate(dueDate)
                        .build();
                installmentRepository.save(holidayBill);
                log.info("Created SKIPPED bill for inactive player {}", p.getId());
                continue; // 🔥 Skip normal bill generation logic for this player
            }
            // ---------------------------------------------------------

            FeeStructure fee = feeStructureService.findEffectiveFeeForGroup(p.getPlayerGroup(), today);
            if (fee == null) {
                log.warn("No fee found for player {} in group {}", p.getId(),
                        p.getPlayerGroup() == null ? "null" : p.getPlayerGroup().getName());
                continue;
            }

            Installment ins = Installment.builder()
                    .player(p)
                    .periodMonth(month)
                    .periodYear(year)
                    .amount(fee.getMonthlyFee())
                    .paidAmount(0.0)
                    .remainingAmount(fee.getMonthlyFee())
                    .status(Installment.Status.PENDING)
                    .dueDate(dueDate)
                    .build();

            installmentRepository.save(ins);
            log.info("Created installment {} for player {}", ins.getId(), p.getId());
        }
    }

    // 1. MANUAL BILL GENERATION
    @Transactional
    public InstallmentResponseDTO createInstallment(InstallmentRequestDTO req) {
        log.info("Creating installment for player {}", req.getPlayerId());
        Player player = playerRepository.findById(req.getPlayerId())
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + req.getPlayerId()));

        // 🔥🔥🔥 FIX: Check if Player is Inactive (Holiday) 🔥🔥🔥
        if (!player.getIsActive()) {
            Installment holidayBill = Installment.builder()
                    .player(player)
                    .periodMonth(req.getPeriodMonth())
                    .periodYear(req.getPeriodYear())
                    .amount(0.0)
                    .paidAmount(0.0)
                    .remainingAmount(0.0)
                    .status(Installment.Status.SKIPPED) // Cyan Chip
                    .notes("Holiday/Paused (Manual Gen)")
                    .dueDate(req.getDueDate())
                    .build();

            Installment saved = installmentRepository.save(holidayBill);
            return toDto(saved);
        }
        // ---------------------------------------------------------

        Double amount = req.getAmount();
        if (amount == null) {
            FeeStructure fee = feeStructureService.findEffectiveFeeForGroup(player.getPlayerGroup(), LocalDate.now());
            if (fee == null)
                throw new IllegalStateException("Cannot create installment: No Fee Structure defined for Group " + player.getPlayerGroup().getName());
            amount = fee.getMonthlyFee();
        }

        // Check date immediately
        LocalDate today = LocalDate.now();
        Installment.Status initialStatus = Installment.Status.PENDING;

        if (req.getDueDate().isBefore(today)) {
            initialStatus = Installment.Status.OVERDUE;
        }

        Installment ins = Installment.builder()
                .player(player)
                .periodMonth(req.getPeriodMonth())
                .periodYear(req.getPeriodYear())
                .amount(amount)
                .paidAmount(0.0)
                .remainingAmount(amount)
                .status(initialStatus)
                .dueDate(req.getDueDate())
                .build();

        Installment saved = installmentRepository.save(ins);
        return toDto(saved);
    }

    public List<InstallmentResponseDTO> getInstallmentsByPlayer(Long playerId) {
        return installmentRepository.findByPlayerId(playerId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public Installment findByIdOrThrow(Long id) {
        return installmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Installment not found: " + id));
    }

    private InstallmentResponseDTO toDto(Installment i) {
        return InstallmentResponseDTO.builder()
                .installmentId(i.getId())
                .playerId(i.getPlayer().getId())
                .playerName(i.getPlayer().getName())
                .periodMonth(i.getPeriodMonth())
                .periodYear(i.getPeriodYear())
                .amount(i.getAmount())
                .paidAmount(i.getPaidAmount())
                .remainingAmount(i.getRemainingAmount())
                .status(i.getStatus() == null ? null : i.getStatus().name())
                .dueDate(i.getDueDate())
                .notes(i.getNotes())
                .build();
    }

    public LatestInstallmentMonthDTO getLatestInstallmentMonth() {
        Object[] row = null;
        try {
            row = installmentRepository.findLatestPeriodNative();
        } catch (Exception e) {
            log.debug("findLatestPeriodNative failed or returned null: {}", e.getMessage());
        }

        if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            return new LatestInstallmentMonthDTO(year, month);
        }

        LocalDate now = LocalDate.now();
        return new LatestInstallmentMonthDTO(now.getYear(), now.getMonthValue());
    }

    /**
     * Checks all installments. If due date is passed and money is left, mark OVERDUE.
     */
    @Transactional
    public void updateOverdueStatuses() {
        LocalDate today = LocalDate.now();
        log.info("Running daily overdue check for date: {}", today);

        List<Installment> overdueCandidates = installmentRepository
                .findByDueDateBeforeAndRemainingAmountGreaterThan(today, 0.0);

        int count = 0;
        for (Installment inst : overdueCandidates) {
            if (inst.getStatus() != Installment.Status.OVERDUE) {
                inst.setStatus(Installment.Status.OVERDUE);
                installmentRepository.save(inst);
                count++;
            }
        }
        log.info("Updated {} installments to OVERDUE status.", count);
    }

    @Transactional
    public InstallmentResponseDTO extendDueDate(InstallmentExtensionDTO req) {
        log.info("Updating due date for installment {} to {}", req.getInstallmentId(), req.getNewDueDate());

        Installment installment = installmentRepository.findById(req.getInstallmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found: " + req.getInstallmentId()));

        installment.setDueDate(req.getNewDueDate());

        // 🔥 FIX: Check BOTH directions (Future & Past)
        if (installment.getRemainingAmount() > 0) {
            LocalDate today = LocalDate.now();

            if (req.getNewDueDate().isBefore(today)) {
                installment.setStatus(Installment.Status.OVERDUE);
                log.info("Date is in past. Status updated to OVERDUE for id {}", installment.getId());
            } else {
                installment.setStatus(Installment.Status.PENDING);
                log.info("Date is in future. Status updated to PENDING for id {}", installment.getId());
            }
        }

        Installment saved = installmentRepository.save(installment);
        return toDto(saved);
    }

    @Transactional
    public void createInstallmentForPlayer(Long playerId, int month, int year, LocalDate dueDate, Double amountOverride) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));

        Double amount = amountOverride;

        if (amount == null) {
            FeeStructure fee = feeStructureService.findEffectiveFeeForGroup(player.getPlayerGroup(), LocalDate.now());
            if (fee == null) {
                log.warn("Skipping auto-installment for player {}: No fee structure found.", playerId);
                return;
            }
            amount = fee.getMonthlyFee();
        }

        Installment.Status initialStatus = Installment.Status.PENDING;
        if (dueDate.isBefore(LocalDate.now())) {
            initialStatus = Installment.Status.OVERDUE;
        }

        Installment ins = Installment.builder()
                .player(player)
                .periodMonth(month)
                .periodYear(year)
                .amount(amount)
                .paidAmount(0.0)
                .remainingAmount(amount)
                .status(initialStatus)
                .dueDate(dueDate)
                .build();

        installmentRepository.save(ins);
        log.info("Auto-generated installment for player {} (Month: {}/{}) Status: {}", playerId, month, year, initialStatus);
    }

    @Transactional
    public String bulkExtendForHolidays(BulkExtendDTO req) {
        long daysToAdd = java.time.temporal.ChronoUnit.DAYS.between(
                req.getHolidayStart(), req.getHolidayEnd()) + 1;

        log.info("Extending due dates starting from {} by {} days due to holiday end {}.",
                req.getHolidayStart(), daysToAdd, req.getHolidayEnd());

        List<Installment> list = installmentRepository.findForFutureExtension(
                req.getHolidayStart(),
                req.getGroupId()
        );

        if (list.isEmpty()) {
            return "No upcoming installments found to extend.";
        }

        for (Installment inst : list) {
            LocalDate oldDate = inst.getDueDate();
            LocalDate newDate = oldDate.plusDays(daysToAdd);
            inst.setDueDate(newDate);

            if (inst.getStatus() == Installment.Status.OVERDUE && !newDate.isBefore(LocalDate.now())) {
                inst.setStatus(Installment.Status.PENDING);
            }
        }

        installmentRepository.saveAll(list);
        return "Applied holiday extension (" + daysToAdd + " days) to " + list.size() + " players.";
    }

    // 🔥🔥🔥 UPDATED REVERT PAYMENT (Handles Refund Status) 🔥🔥🔥
    @Transactional
    public void revertPayment(Long installmentId) {
        log.info("Reversing payment for installment {}", installmentId);

        Installment inst = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new RuntimeException("Installment not found"));

        if (inst.getStatus() != Installment.Status.PAID) {
            throw new RuntimeException("Only PAID bills can be reverted.");
        }

        // 1. History Note
        String historyNote = " | Refunded ₹" + inst.getPaidAmount() + " on " + LocalDate.now();

        // 2. Reset Amounts
        inst.setPaidAmount(0.0);
        inst.setRemainingAmount(inst.getAmount());

        // 3. Set Status to REFUNDED (Instead of PENDING) for Accounting History
        inst.setStatus(Installment.Status.REFUNDED);

        inst.setNotes((inst.getNotes() != null ? inst.getNotes() : "") + historyNote);

        installmentRepository.save(inst);
        log.info("Payment reverted (Refunded) successfully for installment {}", installmentId);
    }

    // 🔥🔥🔥 NEW: CANCEL FUTURE BILLS (Used by PlayerLifecycleService) 🔥🔥🔥
    @Transactional
    public void cancelFutureBills(Long playerId, LocalDate fromDate) {
        log.info("Cancelling future bills for player {} from date {}", playerId, fromDate);

        // Find bills strictly AFTER the date
        List<Installment> futureBills = installmentRepository.findFuturePendingBills(playerId, fromDate);

        for (Installment inst : futureBills) {
            // 🔥 Don't touch PAID or REFUNDED bills
            if (inst.getStatus() == Installment.Status.PAID || inst.getStatus() == Installment.Status.REFUNDED) {
                continue;
            }

            // Mark as CANCELLED
            inst.setStatus(Installment.Status.CANCELLED);
            inst.setAmount(0.0);
            inst.setRemainingAmount(0.0);
            inst.setNotes("Auto-cancelled: Player Left.");

            installmentRepository.save(inst);
        }
    }

    // 🔥🔥🔥 NEW: Adjust Installment Amount (Discount / Correction) 🔥🔥🔥
    @Transactional
    public InstallmentResponseDTO adjustInstallmentAmount(Long installmentId, Double newAmount, String adjustmentReason) {
        log.info("Adjusting installment {} to new amount {}", installmentId, newAmount);

        Installment inst = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new RuntimeException("Installment not found"));

        if (inst.getStatus() == Installment.Status.PAID) {
            throw new RuntimeException("Cannot adjust a fully PAID bill. Please 'Revert Payment' first.");
        }

        if (newAmount < 0) {
            throw new RuntimeException("Amount cannot be negative.");
        }

        Double oldAmount = inst.getAmount();
        inst.setAmount(newAmount);

        // Recalculate remaining (New Amount - Already Paid)
        double newRemaining = newAmount - inst.getPaidAmount();

        if (newRemaining <= 0) {
            newRemaining = 0.0;
            inst.setStatus(Installment.Status.PAID);
        } else {
            // Check overdue if still pending
            if (inst.getDueDate().isBefore(LocalDate.now())) {
                inst.setStatus(Installment.Status.OVERDUE);
            } else {
                inst.setStatus(Installment.Status.PENDING);
            }
        }
        inst.setRemainingAmount(newRemaining);

        String noteEntry = String.format(" | Adjusted: %.0f -> %.0f (%s)", oldAmount, newAmount, adjustmentReason);
        inst.setNotes((inst.getNotes() != null ? inst.getNotes() : "") + noteEntry);

        Installment saved = installmentRepository.save(inst);
        return toDto(saved);
    }
}