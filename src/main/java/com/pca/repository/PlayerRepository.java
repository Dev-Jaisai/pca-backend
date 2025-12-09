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

    // remove any duplicate or incorrect methods named deleteByPlayerId without @Query
    // JpaRepository already provides deleteById(Long id)
}
