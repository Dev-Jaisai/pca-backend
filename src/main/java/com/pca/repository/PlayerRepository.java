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
    List<Player> findByGroupId(Long groupId);
    @Modifying
    @Transactional
    @Query("delete from Payment p where p.installment.player.id = :playerId")
    void deleteByPlayerId(@Param("playerId") Long playerId);

    boolean existsByGroupId(Long groupId);



}
