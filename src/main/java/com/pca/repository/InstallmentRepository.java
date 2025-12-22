package com.pca.repository;

import com.pca.model.Installment;
import com.pca.model.Installment.Status;
import com.pca.repository.proj.PlayerInstallmentSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    boolean existsByPlayerIdAndPeriodMonthAndPeriodYear(Long playerId, Integer periodMonth, Integer periodYear);

    List<Installment> findByStatus(Status status);

    List<Installment> findByPlayerId(Long playerId);

    /**
     * Finds installments whose dueDate is between from (inclusive) and to (inclusive) and remainingAmount > minRemaining.
     * Useful for upcoming and overdue reminder detection.
     */
    List<Installment> findByDueDateBetweenAndRemainingAmountGreaterThan(LocalDate from, LocalDate to, Double minRemaining);

    List<Installment> findByDueDateBeforeAndRemainingAmountGreaterThan(LocalDate before, Double minRemaining);

    @Modifying
    @Transactional
    @Query("delete from Installment i where i.player.id = :playerId")
    void deleteByPlayerId(@Param("playerId") Long playerId);

    // group by player id and return player id + count
    @Query("""
              select i.player.id as playerId, count(i) as cnt
              from Installment i
              where (:month is null or i.periodMonth = :month)
                and (:year is null or i.periodYear = :year)
              group by i.player.id
            """)
    List<Object[]> countInstallmentsGroupedByPlayer(
            @Param("month") Integer month,
            @Param("year") Integer year
    );


    /**
     * Native query that aggregates payments for each installment using period_month and period_year columns.
     * <p>
     * NOTE:
     * - uses actual DB column names: period_month, period_year, player_id, due_date, amount
     * - expects payment.installment_id linking to installment.id
     * - p.join_date assumed to be the column for Player.joinDate
     * <p>
     * Parameters: periodMonth (1..12), periodYear (e.g. 2025)
     */
    @Query(value = "SELECT " +
            " p.id AS playerId, " +
            " p.name AS playerName, " +
            " p.phone AS phone, " +
            " g.name AS groupName, " +
            " p.join_date AS joinDate, " +
            " i.amount AS installmentAmount, " +
            " COALESCE(SUM(pay.amount), 0) AS totalPaid, " +
            " i.due_date AS dueDate, " +
            " i.id AS installmentId " +
            "FROM installment i " +
            "JOIN player p ON i.player_id = p.id " +
            "LEFT JOIN payment pay ON pay.installment_id = i.id " +
            "LEFT JOIN player_group g ON p.group_id = g.id " +
            "WHERE i.period_month = :periodMonth AND i.period_year = :periodYear " +
            "GROUP BY p.id, p.name, p.phone, g.name, p.join_date, i.amount, i.due_date, i.id",
            nativeQuery = true)
    List<PlayerInstallmentSummaryProjection> findSummaryByPeriod(
            @Param("periodMonth") Integer periodMonth,
            @Param("periodYear") Integer periodYear);


    List<Installment> findByPlayerIdAndPeriodMonthAndPeriodYear(Long playerId, int periodMonth, int periodYear);

    // native query to get latest period — adapt column names if different in DB mapping
    @Query(value = "SELECT i.period_year, i.period_month FROM installment i " +
            "ORDER BY i.period_year DESC, i.period_month DESC LIMIT 1",
            nativeQuery = true)
    Object[] findLatestPeriodNative();

    // helper: get installment ids for a player
    @Query("select i.id from Installment i where i.player.id = :playerId")
    List<Long> findIdsByPlayerId(Long playerId);

    @Modifying
    @Transactional
    @Query("delete from Installment i where i.player.id in :playerIds")
    void deleteByPlayerIds(@Param("playerIds") List<Long> playerIds);

    // Also add this method to find installments by multiple player IDs
    @Query("select i from Installment i where i.player.id in :playerIds")
    List<Installment> findByPlayerIds(@Param("playerIds") List<Long> playerIds);

    // Get installments for a player up to a cutoff date (inclusive)
    List<Installment> findByPlayerIdAndDueDateLessThanEqualOrderByDueDateAsc(Long playerId, LocalDate cutoffDate);


    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Installment i WHERE i.player.id = :playerId")
    Double findTotalAmountByPlayerId(@Param("playerId") Long playerId);

    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM Installment i WHERE i.player.id = :playerId")
    Double findTotalPaidByPlayerId(@Param("playerId") Long playerId);

    @Query("SELECT COALESCE(SUM(i.remainingAmount), 0) FROM Installment i WHERE i.player.id = :playerId")
    Double findTotalRemainingByPlayerId(@Param("playerId") Long playerId);

    // Get installments up to current month
    @Query("SELECT i FROM Installment i WHERE i.player.id = :playerId " +
            "AND (i.periodYear < :currentYear OR " +
            "(i.periodYear = :currentYear AND i.periodMonth <= :currentMonth))")
    List<Installment> findInstallmentsUpToCurrentMonth(
            @Param("playerId") Long playerId,
            @Param("currentYear") int currentYear,
            @Param("currentMonth") int currentMonth);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Installment i")
    Double findTotalAmountForAllPlayers();

    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM Installment i")
    Double findTotalPaidForAllPlayers();

    @Query("SELECT COALESCE(SUM(i.remainingAmount), 0) FROM Installment i")
    Double findTotalRemainingForAllPlayers();

    // Get overdue installments (due date passed AND remaining amount > 0)
    @Query("SELECT i FROM Installment i WHERE i.player.id = :playerId " +
            "AND i.dueDate < :today " +
            "AND i.remainingAmount > 0 " +
            "ORDER BY i.periodYear ASC, i.periodMonth ASC")
    List<Installment> findOverdueInstallmentsByPlayer(
            @Param("playerId") Long playerId,
            @Param("today") LocalDate today);

    @Query("SELECT DISTINCT i.player.id FROM Installment i WHERE i.dueDate < CURRENT_DATE AND i.remainingAmount > 0")
    List<Long> findPlayersWithOverdue();

    @Query("SELECT DISTINCT i.player.id FROM Installment i WHERE i.dueDate < :today AND i.remainingAmount > 0")
    List<Long> findPlayersWithOverdue(@Param("today") LocalDate today);

    // Find all unpaid installments for a player
    List<Installment> findByPlayerIdAndStatusNot(Long playerId, Status status);

    // Or use this query if above doesn't work:
    @Query("SELECT i FROM Installment i WHERE i.player.id = :playerId " +
            "AND i.status != 'PAID' " +
            "AND (i.remainingAmount > 0 OR i.remainingAmount IS NULL) " +
            "ORDER BY i.dueDate ASC")
    List<Installment> findUnpaidInstallmentsByPlayer(@Param("playerId") Long playerId);


    @Query("SELECT i FROM Installment i " +
            "JOIN i.player p " +
            "WHERE i.periodMonth = :month " +
            "AND i.periodYear = :year " +
            "AND (:groupId IS NULL OR p.playerGroup.id = :groupId) " +
            "AND i.status != 'PAID'")
    List<Installment> findForBulkExtension(
            @Param("month") int month,
            @Param("year") int year,
            @Param("groupId") Integer groupId
    );

    @Query("SELECT i FROM Installment i " +
            "JOIN i.player p " +
            "WHERE (:groupId IS NULL OR p.playerGroup.id = :groupId) " +
            "AND i.status != 'PAID' " +
            "AND i.dueDate >= :holidayStart") // <-- HE CHANGE KELA (Start Date chya pudhche sagle)
    List<Installment> findForFutureExtension(
            @Param("holidayStart") LocalDate holidayStart,
            @Param("groupId") Integer groupId
    );

    /**
     * Finds the absolute last installment generated for a player
     * (Ordered by Year DESC, Month DESC)
     */
    @Query(value = "SELECT * FROM installment WHERE player_id = :playerId ORDER BY period_year DESC, period_month DESC LIMIT 1", nativeQuery = true)
    Installment findLastByPlayerId(@Param("playerId") Long playerId);
}

