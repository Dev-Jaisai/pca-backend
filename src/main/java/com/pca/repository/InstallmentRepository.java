package com.pca.repository;

import com.pca.model.Installment;
import com.pca.model.Installment.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
