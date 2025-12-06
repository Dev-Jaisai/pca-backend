package com.pca.service;

import com.pca.dto.FeeStructureRequestDTO;
import com.pca.dto.FeeStructureResponseDTO;
import com.pca.exception.ResourceNotFoundException;
import com.pca.model.FeeStructure;
import com.pca.model.GroupEntity;
import com.pca.repository.FeeStructureRepository;
import com.pca.repository.GroupRepository;
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
public class FeeStructureService {

    private final FeeStructureRepository feeRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public FeeStructureResponseDTO createFeeStructure(FeeStructureRequestDTO req) {
        log.info("Creating fee structure for groupId={} amount={}", req.getGroupId(), req.getMonthlyFee());
        GroupEntity group = groupRepository.findById(req.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + req.getGroupId()));

        FeeStructure f = FeeStructure.builder()
                .group(group)
                .monthlyFee(req.getMonthlyFee())
                .effectiveFrom(req.getEffectiveFrom() == null ? LocalDate.now() : req.getEffectiveFrom())
                .effectiveTo(req.getEffectiveTo())
                .build();
        FeeStructure saved = feeRepository.save(f);
        return toDto(saved);
    }

    public List<FeeStructureResponseDTO> getFeeHistory(Long groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
        return feeRepository.findByGroupOrderByEffectiveFromDesc(group)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Fetch the effective fee for the group on the given date.
     */
    public FeeStructure findEffectiveFeeForGroup(GroupEntity group, LocalDate onDate) {
        return feeRepository.findTopByGroupAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(group, onDate)
                .orElse(null);
    }

    private FeeStructureResponseDTO toDto(FeeStructure f) {
        return FeeStructureResponseDTO.builder()
                .id(f.getId())
                .groupId(f.getGroup().getId())
                .groupName(f.getGroup().getName())
                .monthlyFee(f.getMonthlyFee())
                .effectiveFrom(f.getEffectiveFrom())
                .effectiveTo(f.getEffectiveTo())
                .build();
    }
}
