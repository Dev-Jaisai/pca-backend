package com.pca.controller;

import com.pca.dto.FeeStructureRequestDTO;
import com.pca.dto.FeeStructureResponseDTO;
import com.pca.service.FeeStructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
@Slf4j
public class FeeStructureController {

    private final FeeStructureService feeService;

    @PostMapping
    public FeeStructureResponseDTO create(@Valid @RequestBody FeeStructureRequestDTO request) {
        log.info("Creating fee structure for group {}", request.getGroupId());
        return feeService.createFeeStructure(request);
    }

    @GetMapping("/group/{groupId}")
    public List<FeeStructureResponseDTO> getAllByGroup(@PathVariable Long groupId) {
        return feeService.getFeeHistory(groupId);
    }
}
