package com.pca.service;

import com.pca.dto.PlayerRequestDTO;
import com.pca.dto.PlayerResponseDTO;
import com.pca.exception.ResourceNotFoundException;
import com.pca.model.GroupEntity;
import com.pca.model.Player;
import com.pca.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final GroupRepository groupRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentRepository paymentRepository;
    private final ReminderHistoryRepository reminderHistoryRepository;

    // ✅ IMP: InstallmentService Inject केला आहे (Immediate Bill साठी)
    private final InstallmentService installmentService;

    @Transactional
    public PlayerResponseDTO createPlayer(PlayerRequestDTO req) {
        log.info("Creating player {}", req.getName());

        // 1. Group check kara
        GroupEntity group = groupRepository.findById(req.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + req.getGroupId()));

        // 2. Player Object banva
        Player p = Player.builder()
                .name(req.getName())
                .phone(req.getPhone())
                .age(req.getAge())
                .joinDate(req.getJoinDate())
                .playerGroup(group)
                .notes(req.getNotes())
                .photoUrl(req.getPhotoUrl())
                .build();

        // -----------------------------------------------------------
        // 🔥 LOGIC 1: PAYMENT CYCLE (Monthly / Quarterly)
        // -----------------------------------------------------------
        if (req.getPaymentCycleMonths() != null && req.getPaymentCycleMonths() > 0) {
            p.setPaymentCycleMonths(req.getPaymentCycleMonths());
        } else {
            p.setPaymentCycleMonths(1); // Default Monthly (1)
        }

        // -----------------------------------------------------------
        // 🔥 LOGIC 2: BILLING DAY & FIRST DUE DATE
        // -----------------------------------------------------------
        LocalDate firstDueDate;
        if (req.getFirstInstallmentDate() != null) {
            // Coach ne dili ti date vapra
            firstDueDate = req.getFirstInstallmentDate();
        } else {
            // Coach ne dili nahi, tar AAJCHI date vapra (Immediate Payment)
            firstDueDate = LocalDate.now();
        }

        // Billing Day set kara (He kayam fix rahil)
        p.setBillingDay(firstDueDate.getDayOfMonth());


        // 3. Player Save kara
        Player saved = playerRepository.save(p);

        // -----------------------------------------------------------
        // 🔥 LOGIC 3: IMMEDIATE FIRST INSTALLMENT GENERATION
        // (Cron Job chi vat na baghta lagech bill banva)
        // -----------------------------------------------------------
        try {
            log.info("Generating immediate first installment for {}", saved.getName());

            installmentService.createInstallmentForPlayer(
                    saved.getId(),
                    firstDueDate.getMonthValue(), // Month
                    firstDueDate.getYear(),       // Year
                    firstDueDate,                 // Due Date
                    null                          // Amount (Null = Auto from Group Fee)
            );
        } catch (Exception e) {
            // Error alyas log kara, pan player creation roll-back naka karu
            log.error("Failed to create first installment for {}: {}", saved.getName(), e.getMessage());
        }

        return toDto(saved);
    }

    public List<PlayerResponseDTO> getAllPlayers() {
        return playerRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public PlayerResponseDTO getPlayerById(Long id) {
        Player p = playerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
        return toDto(p);
    }
    @Transactional
    public PlayerResponseDTO updatePlayer(Long id, PlayerRequestDTO req) {
        log.info("Updating player id={}", id);

        Player p = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));

        // 1. Basic Info Update
        if (req.getName() != null) p.setName(req.getName());
        if (req.getPhone() != null) p.setPhone(req.getPhone());
        if (req.getAge() != null) p.setAge(req.getAge());
        if (req.getJoinDate() != null) p.setJoinDate(req.getJoinDate());

        if (req.getGroupId() != null) {
            GroupEntity group = groupRepository.findById(req.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + req.getGroupId()));
            p.setPlayerGroup(group);
        }

        if (req.getNotes() != null) p.setNotes(req.getNotes());
        if (req.getPhotoUrl() != null) p.setPhotoUrl(req.getPhotoUrl());

        // -----------------------------------------------------------
        // 🔥 NEW: BILLING UPDATE LOGIC
        // -----------------------------------------------------------

        // 2. Update Payment Cycle (Monthly -> Quarterly or vice versa)
        if (req.getPaymentCycleMonths() != null && req.getPaymentCycleMonths() > 0) {
            p.setPaymentCycleMonths(req.getPaymentCycleMonths());
            log.info("Updated Payment Cycle for player {}: {} months", id, req.getPaymentCycleMonths());
        }

        // 3. Update Billing Day (Change future due date day)
        // जर युजरने 'firstInstallmentDate' पाठवली, तर आपण त्याचा 'दिवस' (Day)
        // नवीन Billing Day म्हणून सेट करू.
        if (req.getFirstInstallmentDate() != null) {
            int newBillDay = req.getFirstInstallmentDate().getDayOfMonth();
            p.setBillingDay(newBillDay);
            log.info("Updated Billing Day for player {}: Day {}", id, newBillDay);
        }

        Player updated = playerRepository.save(p);
        return toDto(updated);
    }

    @Transactional
    public void deletePlayer(Long playerId) {
        log.info("Deleting player {} and related data", playerId);
        paymentRepository.deleteByPlayerId(playerId);

        List<Long> instIds = installmentRepository.findIdsByPlayerId(playerId);
        if (!instIds.isEmpty()) {
            reminderHistoryRepository.deleteByInstallmentIdIn(instIds);
            installmentRepository.deleteByPlayerId(playerId);
        }

        playerRepository.deleteById(playerId);
    }

    public Player findByIdOrThrow(Long id) {
        return playerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }
    // खालील मेथड रिप्लेस करा
    private PlayerResponseDTO toDto(Player p) {
        return PlayerResponseDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .phone(p.getPhone())
                .age(p.getAge())
                .joinDate(p.getJoinDate())
                .groupId(p.getPlayerGroup() != null ? p.getPlayerGroup().getId() : null)
                .groupName(p.getPlayerGroup() != null ? p.getPlayerGroup().getName() : null)
                .notes(p.getNotes())
                .photoUrl(p.getPhotoUrl())

                // ✅ हे ऍड करा: आता Backend फ्रंटेंडला डेटा पाठवेल
                .billingDay(p.getBillingDay())
                .paymentCycleMonths(p.getPaymentCycleMonths())
                .isActive(p.getIsActive()) // OR p.isActive() depending on your getter

                .build();
    }
}