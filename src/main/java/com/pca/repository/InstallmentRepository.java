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
}

