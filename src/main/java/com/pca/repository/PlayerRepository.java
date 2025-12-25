package com.pca.repository;

import com.pca.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    @Query("SELECT p.id FROM Player p WHERE p.playerGroup.id = :groupId")
    List<Long> findIdByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Player p WHERE p.playerGroup.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);

    boolean existsByPlayerGroupId(Long groupId);

    // FIX 1: Join Day logic
    @Query(value = "SELECT * FROM player WHERE DAY(join_date) = :day", nativeQuery = true)
    List<Player> findByJoinDay(@Param("day") int day);

    // FIX 2: Billing Day logic
    @Query(value = "SELECT * FROM player WHERE billing_day <= :day", nativeQuery = true)
    List<Player> findByBillingDayLessThanEqual(@Param("day") int day);

    // 🔥 NEW METHOD FOR SCHEDULER (FAKT ACTIVE PLAYERS)
    // Billing Day check kara + Active aahet ka te check kara
    @Query(value = "SELECT * FROM player WHERE billing_day <= :day AND is_active = true", nativeQuery = true)
    List<Player> findActivePlayersByBillingDay(@Param("day") int day);
}