package com.pca.controller;

import com.pca.dto.PlayerRequestDTO;
import com.pca.dto.PlayerResponseDTO;
import com.pca.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@Slf4j
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    public PlayerResponseDTO create(@Valid @RequestBody PlayerRequestDTO request) {
        log.info("Creating player {}", request.getName());
        return playerService.createPlayer(request);
    }

    @GetMapping
    public List<PlayerResponseDTO> getAll() {
        return playerService.getAllPlayers();
    }

    @GetMapping("/{id}")
    public PlayerResponseDTO getById(@PathVariable Long id) {
        return playerService.getPlayerById(id);
    }

    @PutMapping("/{id}")
    public PlayerResponseDTO update(@PathVariable Long id, @Valid @RequestBody PlayerRequestDTO request) {
        return playerService.updatePlayer(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        playerService.deletePlayer(id);
    }
}
