package com.pca.repository;

import com.pca.model.Player;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerInstallmentSummaryRepository extends Repository<Player, Long> {

    // 1️⃣ Month-Specific
    @Query(value = """
        SELECT 
            p.id AS playerId, 
            p.name AS playerName, 
            p.phone AS phone, 
            g.name AS groupName, 
            p.join_date AS joinDate,
            i.id AS installmentId, 
            i.amount AS installmentAmount, 
            i.paid_amount AS totalPaid,
            i.due_date AS dueDate, 
            i.status AS status, 
            i.remaining_amount AS remaining, 
            MAX(pay.paid_on) AS lastPaymentDate
        FROM player p
        LEFT JOIN player_group g ON p.group_id = g.id
        JOIN installment i ON i.player_id = p.id 
            AND i.period_year = :year 
            AND i.period_month = :month
        LEFT JOIN payment pay ON pay.installment_id = i.id
        GROUP BY p.id, p.name, p.phone, g.name, p.join_date,
                 i.id, i.amount, i.paid_amount, i.due_date, i.status, i.remaining_amount
        ORDER BY p.name ASC
        """, nativeQuery = true)
    List<Object[]> fetchSummary(@Param("year") int year, @Param("month") int month);

    // 2️⃣ All-Time Query
    @Query(value = """
        SELECT 
            p.id AS playerId,
            p.name AS playerName,
            p.phone AS phone,
            g.name AS groupName,
            p.join_date AS joinDate,
            i.id AS installmentId,
            i.amount AS installmentAmount,
            i.paid_amount AS totalPaid,
            i.due_date AS dueDate,
            i.status AS status,
            i.remaining_amount AS remaining,
            MAX(pay.paid_on) AS lastPaymentDate,
            i.notes AS notes
        FROM player p
        LEFT JOIN player_group g ON p.group_id = g.id
        JOIN installment i ON i.player_id = p.id
        LEFT JOIN payment pay ON pay.installment_id = i.id
        GROUP BY p.id, p.name, p.phone, g.name, p.join_date,
                 i.id, i.amount, i.paid_amount, i.due_date, i.status, i.remaining_amount, i.notes
        ORDER BY i.due_date DESC
        """, nativeQuery = true)
    List<Object[]> fetchAllSummary();

    // 3️⃣ Paginated Query
    @Query(value = """
        SELECT 
            p.id AS playerId,
            p.name AS playerName,
            p.phone AS phone,
            g.name AS groupName,
            p.join_date AS joinDate,
            i.id AS installmentId,
            i.amount AS installmentAmount,
            i.paid_amount AS totalPaid,
            i.due_date AS dueDate,
            i.status AS status,
            i.remaining_amount AS remaining,
            MAX(pay.paid_on) AS lastPaymentDate,
            i.notes AS notes
        FROM player p
        LEFT JOIN player_group g ON p.group_id = g.id
        JOIN installment i ON i.player_id = p.id
        LEFT JOIN payment pay ON pay.installment_id = i.id
        GROUP BY p.id, p.name, p.phone, g.name, p.join_date,
                 i.id, i.amount, i.paid_amount, i.due_date, i.status, i.remaining_amount, i.notes
        ORDER BY 
            CASE 
                WHEN i.status = 'PENDING' THEN 1 
                WHEN i.status = 'PARTIALLY_PAID' THEN 2 
                WHEN i.status = 'OVERDUE' THEN 3 
                ELSE 4 
            END ASC,
            i.due_date ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> fetchAllSummaryPaginated(@Param("limit") int limit, @Param("offset") long offset);

    @Query(value = "SELECT COUNT(*) FROM installment", nativeQuery = true)
    long countAllInstallments();

    // 4️⃣ Overdue Query
    @Query(value = """
        SELECT 
            p.id AS playerId,
            p.name AS playerName,
            p.phone AS phone,
            g.name AS groupName,
            p.join_date AS joinDate,
            SUM(i.amount) AS totalInstallmentAmount,
            COUNT(i.id) AS installmentCount,
            SUM(i.paid_amount) AS totalPaid,
            MAX(i.due_date) AS latestDueDate,
            GROUP_CONCAT(DISTINCT i.status) AS statuses,
            SUM(i.remaining_amount) AS totalRemaining,
            MAX(pay.paid_on) AS lastPaymentDate
        FROM player p
        LEFT JOIN player_group g ON p.group_id = g.id
        JOIN installment i ON i.player_id = p.id
        LEFT JOIN payment pay ON pay.installment_id = i.id
        WHERE i.due_date < CURDATE() AND i.remaining_amount > 0
        GROUP BY p.id, p.name, p.phone, g.name, p.join_date
        ORDER BY latestDueDate DESC
        """, nativeQuery = true)
    List<Object[]> fetchOverdueSummary();
}