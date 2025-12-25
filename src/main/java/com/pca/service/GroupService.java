package com.pca.service;

import com.pca.dto.GroupRequestDTO;
import com.pca.dto.GroupResponseDTO;
import com.pca.exception.ResourceNotFoundException;
import com.pca.model.FeeStructure;
import com.pca.model.GroupEntity;
import com.pca.model.Installment;
import com.pca.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
        log.info("Creating group '{}' with fee {}", request.getName(), request.getMonthlyFee());

        // 1. Group Save करा
        GroupEntity g = GroupEntity.builder()
                .name(request.getName().trim())
                .build();
        GroupEntity savedGroup = groupRepository.save(g);

        // 2. Fee Structure Save करा (Link to Group)
        FeeStructure fee = FeeStructure.builder()
                .group(savedGroup)
                .monthlyFee(request.getMonthlyFee())
                .effectiveFrom(LocalDate.now()) // आजपासून लागू
                .build();
        feeStructureRepository.save(fee);

        // 3. Response रिटर्न करा
        return toDto(savedGroup, request.getMonthlyFee());
    }

    public List<GroupResponseDTO> getAllGroups() {
        List<GroupEntity> groups = groupRepository.findAll();
        return groups.stream().map(g -> {
            // लेटेस्ट फी शोधण्यासाठी (Optional logic, performance साठी direct query वापरू शकता)
            Optional<FeeStructure> latestFee = feeStructureRepository
                    .findTopByGroupAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(g, LocalDate.now());

            Double feeAmount = latestFee.map(FeeStructure::getMonthlyFee).orElse(0.0);
            return toDto(g, feeAmount);
        }).collect(Collectors.toList());
    }

    public GroupEntity findByIdOrThrow(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + id));
    }

    private GroupResponseDTO toDto(GroupEntity e, Double fee) {
        return GroupResponseDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .currentFee(fee)
                .build();
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        log.info("Deleting group with id={}", groupId);

        // 1. Validate existence (Keep this to ensure 404 if not found)
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found: " + groupId);
        }

        // 2. Delete Players (and their children)
        List<Long> playerIds = playerRepository.findIdByGroupId(groupId);
        if (!playerIds.isEmpty()) {
            // ... (Installment cleanup logic remains same) ...
            List<Installment> installments = installmentRepository.findByPlayerIds(playerIds);
            List<Long> installmentIds = installments.stream().map(Installment::getId).toList();

            if (!installmentIds.isEmpty()) {
                reminderHistoryRepository.deleteByInstallmentIds(installmentIds);
                paymentRepository.deleteByInstallmentIds(installmentIds);
                installmentRepository.deleteByPlayerIds(playerIds);
            }
            playerRepository.deleteByGroupId(groupId);
        }

        // 3. Delete Fee Structures
        // Use the native query delete to avoid loading entities
        feeStructureRepository.deleteByGroupId(groupId);

        // 4. FIX IS HERE: Delete Group by ID directly
        // OLD: groupRepository.delete(group);  <-- This caused the error
        // NEW:
        groupRepository.deleteGroupCustom(groupId);

        log.info("Successfully deleted group id={}", groupId);
    }

    @Transactional
    public GroupResponseDTO updateGroup(Long groupId, GroupRequestDTO request) {
        log.info("Updating group id={} with name={} and fee={}", groupId, request.getName(), request.getMonthlyFee());

        // 1. Group शोधा
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        // 2. नाव अपडेट करा
        group.setName(request.getName().trim());
        GroupEntity savedGroup = groupRepository.save(group);

        // 3. Latest Fee शोधा आणि अपडेट करा
        // आपण फक्त आजच्या तारखेला Active असलेली किंवा लेटेस्ट फी अपडेट करू
        Optional<FeeStructure> latestFeeOpt = feeStructureRepository
                .findTopByGroupAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(group, LocalDate.now());

        if (latestFeeOpt.isPresent()) {
            FeeStructure fee = latestFeeOpt.get();
            fee.setMonthlyFee(request.getMonthlyFee());
            feeStructureRepository.save(fee);
        } else {
            // जर फी स्ट्रक्चर नसेल तर नवीन बनवा (Error handling precaution)
            FeeStructure newFee = FeeStructure.builder()
                    .group(group)
                    .monthlyFee(request.getMonthlyFee())
                    .effectiveFrom(LocalDate.now())
                    .build();
            feeStructureRepository.save(newFee);
        }

        return toDto(savedGroup, request.getMonthlyFee());
    }
}