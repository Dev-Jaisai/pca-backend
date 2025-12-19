package com.pca.controller;

import com.pca.dto.FeeStructureRequestDTO;
import com.pca.dto.FeeStructureResponseDTO;
import com.pca.service.FeeStructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
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

    // FeeStructureController.java (add)
    @GetMapping("/group/{groupId}/effective")
    public ResponseEntity<FeeStructureResponseDTO> getEffective(
            @PathVariable Long groupId,
            @RequestParam(required = false) String date // yyyy-MM-dd optional
    ) {
        LocalDate onDate = (date == null) ? LocalDate.now() : LocalDate.parse(date);
        FeeStructureResponseDTO dto = feeService.getEffectiveFeeForGroup(groupId, onDate);
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // or 404 if you prefer
        }
        return ResponseEntity.ok(dto);
    }
    @PutMapping("/{id}")
    public ResponseEntity<FeeStructureResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FeeStructureRequestDTO request
    ) {
        log.info("Updating fee structure id={}", id);
        return ResponseEntity.ok(feeService.updateFeeStructure(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deleting fee structure id={}", id);
        feeService.deleteFeeStructure(id);
        return ResponseEntity.noContent().build();
    }
}
