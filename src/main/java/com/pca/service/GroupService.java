package com.pca.service;

import com.pca.dto.GroupRequestDTO;
import com.pca.dto.GroupResponseDTO;
import com.pca.exception.ResourceNotFoundException;
import com.pca.model.GroupEntity;
import com.pca.repository.GroupRepository;
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
}
