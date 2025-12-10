package com.pca.repository;

import com.pca.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository used only to run the native summary query.
 * Player is the managed type so Spring Data can create a bean.
 * The method returns List<Object[]> (native rows) — service maps to DTO.
 */
public interface InstallmentSummaryRepository extends JpaRepository<Player, Long> {

    @Query(value = """
        SELECT 
            p.id                AS playerId,
            p.name              AS playerName,
            p.phone             AS phone,
            g.name              AS groupName,
            p.join_date         AS joinDate,
            
            i.id                AS installmentId,
            i.amount            AS installmentAmount,
            COALESCE(SUM(pay.amount), 0) AS totalPaid,
            i.due_date          AS dueDate

        FROM player p
        LEFT JOIN player_group g ON p.group_id = g.id
        LEFT JOIN installment i 
               ON i.player_id = p.id 
              AND i.period_year = :year 
              AND i.period_month = :month
        LEFT JOIN payment pay 
               ON pay.installment_id = i.id

        GROUP BY p.id, p.name, p.phone, g.name, p.join_date, i.id, i.amount, i.due_date
        ORDER BY p.name ASC
        """, nativeQuery = true)
    List<Object[]> findSummaryRowsForMonth(@Param("year") int year, @Param("month") int month);
}
