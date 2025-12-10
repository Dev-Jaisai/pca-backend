// com.pca.service.InstallmentStatusService.java
package com.pca.service;

import com.pca.dto.InstallmentStatusDTO;
import com.pca.model.Player;
import com.pca.repository.InstallmentRepository;
import com.pca.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstallmentStatusService {

    private final InstallmentRepository installmentRepository;
    private final PlayerRepository playerRepository;

    /**
     * Returns list of InstallmentStatusDTO for ALL players.
     * If month/year are provided (non-null) only installments in that month/year are considered.
     */
    public List<InstallmentStatusDTO> getInstallmentStatusForAllPlayers(Integer month, Integer year) {
        // fetch counts grouped by player (for the optional month/year)
        List<Object[]> rows = installmentRepository.countInstallmentsGroupedByPlayer(month, year);

        // convert to map: playerId -> count
        Map<Long, Long> countMap = rows.stream().collect(Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> ((Number) r[1]).longValue()
        ));

        // fetch all players and produce DTO list
        List<Player> players = playerRepository.findAll();

        List<InstallmentStatusDTO> result = new ArrayList<>(players.size());
        for (Player p : players) {
            boolean has = countMap.getOrDefault(p.getId(), 0L) > 0L;
            result.add(new InstallmentStatusDTO(p.getId(), has));
        }
        return result;
    }
}
