package com.pca.controller;

import com.pca.dto.GroupRequestDTO;
import com.pca.dto.GroupResponseDTO;
import com.pca.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public GroupResponseDTO create(@Valid @RequestBody GroupRequestDTO request) {
        log.info("Creating group {}", request.getName());
        return groupService.createGroup(request);
    }

    @GetMapping
    public List<GroupResponseDTO> getAll() {
        return groupService.getAllGroups();
    }
}
