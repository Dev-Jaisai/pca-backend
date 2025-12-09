package com.pca.service;

import com.pca.dto.GroupRequestDTO;
import com.pca.dto.GroupResponseDTO;
import com.pca.exception.ResourceNotFoundException;
import com.pca.model.GroupEntity;
import com.pca.model.Installment;
import com.pca.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final PlayerRepository playerRepository;
    private final FeeStructureRepository feeStructureRepository; // NEW
    private final InstallmentRepository installmentRepository;
    private final ReminderHistoryRepository reminderHistoryRepository;
    private final PaymentRepository paymentRepository;
    @Transactional
    public GroupResponseDTO createGroup(GroupRequestDTO request) {
        log.info("Creating group with name={}", request.getName());
        GroupEntity g = GroupEntity.builder()
                .name(request.getName().trim())
                .build();
        GroupEntity saved = groupRepository.save(g);
        return toDto(saved);
    }

    public List<GroupResponseDTO> getAllGroups() {
        return groupRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public GroupEntity findByIdOrThrow(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + id));
    }

    private GroupResponseDTO toDto(GroupEntity e) {
        return GroupResponseDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .build();
    }
    @Transactional
    public void deleteGroup(Long groupId) {
        log.info("Deleting group with id={}", groupId);

        // Check if group exists
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        // Step 1: Get all player IDs in this group
        List<Long> playerIds = playerRepository.findIdByGroupId(groupId);

        if (!playerIds.isEmpty()) {
            log.debug("Found {} players in group, processing cascade delete", playerIds.size());

            // Step 2: Get all installment IDs for these players
            List<Installment> installments = installmentRepository.findByPlayerIds(playerIds);
            List<Long> installmentIds = installments.stream()
                    .map(Installment::getId)
                    .collect(Collectors.toList());

            if (!installmentIds.isEmpty()) {
                log.debug("Found {} installments, deleting related records", installmentIds.size());

                // Step 3: Delete reminder histories (if any)
                try {
                    reminderHistoryRepository.deleteByInstallmentIds(installmentIds);
                    log.debug("Deleted reminder histories");
                } catch (Exception e) {
                    log.warn("Error deleting reminder histories: {}", e.getMessage());
                    // Continue with other deletions
                }

                // Step 4: Delete payments (if any)
                try {
                    paymentRepository.deleteByInstallmentIds(installmentIds);
                    log.debug("Deleted payments");
                } catch (Exception e) {
                    log.warn("Error deleting payments: {}", e.getMessage());
                    // Continue with other deletions
                }

                // Step 5: Delete installments
                try {
                    installmentRepository.deleteByPlayerIds(playerIds);
                    log.debug("Deleted installments");
                } catch (Exception e) {
                    log.warn("Error deleting installments: {}", e.getMessage());
                    throw new IllegalStateException("Failed to delete installments: " + e.getMessage());
                }
            } else {
                log.debug("No installments found for players in group");
            }

            // Step 6: Delete players
            try {
                playerRepository.deleteByGroupId(groupId);
                log.debug("Deleted players");
            } catch (Exception e) {
                log.warn("Error deleting players: {}", e.getMessage());
                throw new IllegalStateException("Failed to delete players: " + e.getMessage());
            }
        } else {
            log.debug("No players found in group");
        }

        // Step 7: Delete fee structures (if any)
        try {
            feeStructureRepository.deleteByGroupId(groupId);
            log.debug("Deleted fee structures");
        } catch (Exception e) {
            log.warn("Error deleting fee structures: {}", e.getMessage());
            // Continue with group deletion
        }

        // Step 8: Delete the group
        groupRepository.delete(group);

        log.info("Successfully deleted group with id={}", groupId);
    }
}
