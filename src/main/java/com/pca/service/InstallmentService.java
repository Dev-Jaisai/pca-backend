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

            // FIX 1: Updated p.getGroup() to p.getPlayerGroup()
            FeeStructure fee = feeStructureService.findEffectiveFeeForGroup(p.getPlayerGroup(), today);
            if (fee == null) {
                // FIX 2: Updated p.getGroup() to p.getPlayerGroup() for logging
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

    @Transactional
    public InstallmentResponseDTO createInstallment(InstallmentRequestDTO req) {
        log.info("Creating installment for player {}", req.getPlayerId());
        Player player = playerRepository.findById(req.getPlayerId())
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + req.getPlayerId()));

        Double amount = req.getAmount();
        if (amount == null) {
            // FIX 3: Updated player.getGroup() to player.getPlayerGroup()
            FeeStructure fee = feeStructureService.findEffectiveFeeForGroup(player.getPlayerGroup(), LocalDate.now());
            if (fee == null) throw new IllegalArgumentException("No fee structure for player's group");
            amount = fee.getMonthlyFee();
        }

        Installment ins = Installment.builder()
                .player(player)
                .periodMonth(req.getPeriodMonth())
                .periodYear(req.getPeriodYear())
                .amount(amount)
                .paidAmount(0.0)
                .remainingAmount(amount)
                .status(Installment.Status.PENDING)
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

        // Find installments where dueDate is before today AND remainingAmount > 0
        List<Installment> overdueCandidates = installmentRepository
                .findByDueDateBeforeAndRemainingAmountGreaterThan(today, 0.0);

        int count = 0;
        for (Installment inst : overdueCandidates) {
            // Only update if it is not already marked as OVERDUE
            if (inst.getStatus() != Installment.Status.OVERDUE) {
                inst.setStatus(Installment.Status.OVERDUE);
                installmentRepository.save(inst);
                count++;
            }
        }
        log.info("Updated {} installments to OVERDUE status.", count);
    }

    // --- ADD THIS METHOD ---
    @Transactional
    public InstallmentResponseDTO extendDueDate(InstallmentExtensionDTO req) {
        log.info("Updating due date for installment {} to {}", req.getInstallmentId(), req.getNewDueDate());

        Installment installment = installmentRepository.findById(req.getInstallmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found: " + req.getInstallmentId()));

        // 1. Set New Date Directly
        installment.setDueDate(req.getNewDueDate());

        // 2. Smart Status Reset
        // If it was OVERDUE, but the new date is today or future, make it PENDING.
        if (installment.getStatus() == Installment.Status.OVERDUE) {
            LocalDate today = LocalDate.now();
            if (!req.getNewDueDate().isBefore(today)) {
                installment.setStatus(Installment.Status.PENDING);
                log.info("Auto-correcting status to PENDING for installment {}", installment.getId());
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

        // If no specific amount provided, fetch the group fee
        if (amount == null) {
            FeeStructure fee = feeStructureService.findEffectiveFeeForGroup(player.getPlayerGroup(), LocalDate.now());
            if (fee == null) {
                log.warn("Skipping auto-installment for player {}: No fee structure found.", playerId);
                return; // Exit safely if no fee is defined
            }
            amount = fee.getMonthlyFee();
        }

        Installment ins = Installment.builder()
                .player(player)
                .periodMonth(month)
                .periodYear(year)
                .amount(amount)
                .paidAmount(0.0)
                .remainingAmount(amount)
                .status(Installment.Status.PENDING)
                .dueDate(dueDate)
                .build();

        installmentRepository.save(ins);
        log.info("Auto-generated installment for player {} (Month: {}/{})", playerId, month, year);
    }
//
//    @Transactional
//    public String bulkExtendDueDate(com.pca.dto.BulkExtendDTO req) {
//        log.info("Bulk extending due dates. Group: {}, Days: {}, Month: {}/{}",
//                req.getGroupId(), req.getDaysToAdd(), req.getMonth(), req.getYear());
//
//        // 1. Installments shodha (Paid soḍun baki sagle)
//        List<Installment> list = installmentRepository.findForBulkExtension(
//                req.getMonth(),
//                req.getYear(),
//                req.getGroupId()
//        );
//
//        if (list.isEmpty()) {
//            return "No pending installments found for this selection.";
//        }
//
//        int count = 0;
//        LocalDate today = LocalDate.now();
//
//        for (Installment inst : list) {
//            // 2. Junya date madhye divas add kara
//            LocalDate oldDate = inst.getDueDate();
//            if (oldDate == null) continue; // Safety check
//
//            LocalDate newDate = oldDate.plusDays(req.getDaysToAdd());
//            inst.setDueDate(newDate);
//
//            // 3. Status Reset Logic (Imp!)
//            // Jar status OVERDUE hota, pan navin date future madhye ahe, tar PENDING kara.
//            if (inst.getStatus() == Installment.Status.OVERDUE) {
//                if (!newDate.isBefore(today)) { // Jar aaj kiva future date ahe
//                    inst.setStatus(Installment.Status.PENDING);
//                }
//            }
//            count++;
//        }
//
//        installmentRepository.saveAll(list);
//        return "Successfully extended due dates for " + count + " players.";
//    }
    @Transactional
    public String bulkExtendForHolidays(BulkExtendDTO req) {
        // 1. Divas Calculate kara
        long daysToAdd = java.time.temporal.ChronoUnit.DAYS.between(
                req.getHolidayStart(), req.getHolidayEnd()) + 1;

        log.info("Extending due dates starting from {} by {} days due to holiday end {}.",
                req.getHolidayStart(), daysToAdd, req.getHolidayEnd());

        // 2. Query Call (Start date chya pudhche sagle pending items)
        List<Installment> list = installmentRepository.findForFutureExtension(
                req.getHolidayStart(), // Fakt start date pathva
                req.getGroupId()
        );

        if (list.isEmpty()) {
            return "No upcoming installments found to extend.";
        }

        // 3. Update Dates
        for (Installment inst : list) {
            LocalDate oldDate = inst.getDueDate();
            LocalDate newDate = oldDate.plusDays(daysToAdd);
            inst.setDueDate(newDate);

            // Status update: Jar chukun overdue disat asel pan navin date future madhye geli, tar Pending kara
            if (inst.getStatus() == Installment.Status.OVERDUE && !newDate.isBefore(LocalDate.now())) {
                inst.setStatus(Installment.Status.PENDING);
            }
        }

        installmentRepository.saveAll(list);
        return "Applied holiday extension (" + daysToAdd + " days) to " + list.size() + " players.";
    }

}