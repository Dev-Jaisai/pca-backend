package com.pca.repository;

import com.pca.model.Installment;
import com.pca.model.Installment.Status;
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
}
