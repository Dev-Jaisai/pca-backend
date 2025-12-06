package com.pca.service;

import com.pca.dto.PlayerRequestDTO;
import com.pca.dto.PlayerResponseDTO;
import com.pca.exception.ResourceNotFoundException;
import com.pca.model.GroupEntity;
import com.pca.model.Player;
import com.pca.repository.GroupRepository;
import com.pca.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public PlayerResponseDTO createPlayer(PlayerRequestDTO req) {
        log.info("Creating player {}", req.getName());
        GroupEntity group = groupRepository.findById(req.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + req.getGroupId()));

        Player p = Player.builder()
                .name(req.getName())
                .phone(req.getPhone())
                .age(req.getAge())
                .joinDate(req.getJoinDate())
                .group(group)
                .notes(req.getNotes())
                .photoUrl(req.getPhotoUrl())
                .build();

        Player saved = playerRepository.save(p);
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
        Player p = playerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
        if (req.getName() != null) p.setName(req.getName());
        p.setPhone(req.getPhone());
        p.setAge(req.getAge());
        p.setJoinDate(req.getJoinDate());
        if (req.getGroupId() != null) {
            GroupEntity group = groupRepository.findById(req.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + req.getGroupId()));
            p.setGroup(group);
        }
        p.setNotes(req.getNotes());
        p.setPhotoUrl(req.getPhotoUrl());
        Player updated = playerRepository.save(p);
        return toDto(updated);
    }

    @Transactional
    public void deletePlayer(Long id) {
        log.info("Deleting player {}", id);
        if (!playerRepository.existsById(id)) throw new ResourceNotFoundException("Player not found: " + id);
        playerRepository.deleteById(id);
    }

    public Player findByIdOrThrow(Long id) {
        return playerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }

    private PlayerResponseDTO toDto(Player p) {
        return PlayerResponseDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .phone(p.getPhone())
                .age(p.getAge())
                .joinDate(p.getJoinDate())
                .groupId(p.getGroup() != null ? p.getGroup().getId() : null)
                .groupName(p.getGroup() != null ? p.getGroup().getName() : null)
                .notes(p.getNotes())
                .photoUrl(p.getPhotoUrl())
                .build();
    }
}
