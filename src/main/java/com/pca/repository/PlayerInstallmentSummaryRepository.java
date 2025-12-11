package com.pca.repository;

import com.pca.model.Player;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerInstallmentSummaryRepository extends Repository<Player, Long> {

    // 1. Month-Specific Query
    @Query(value = """
        SELECT 
            p.id AS playerId, p.name AS playerName, p.phone AS phone, g.name AS groupName, p.join_date AS joinDate,
            i.id AS installmentId, i.amount AS installmentAmount, COALESCE(SUM(pay.amount), 0) AS totalPaid,
            i.due_date AS dueDate, i.status AS status, i.remaining_amount AS remaining, MAX(pay.paid_on) AS lastPaymentDate
        FROM player p
        LEFT JOIN player_group g ON p.group_id = g.id
        
        -- CHANGE THIS FROM LEFT JOIN TO JOIN (Inner Join)
        JOIN installment i ON i.player_id = p.id AND i.period_year = :year AND i.period_month = :month
        
        LEFT JOIN payment pay ON pay.installment_id = i.id
        GROUP BY p.id, p.name, p.phone, g.name, p.join_date, i.id, i.amount, i.due_date, i.status, i.remaining_amount
        ORDER BY p.name ASC
        """, nativeQuery = true)
    List<Object[]> fetchSummary(@Param("year") int year, @Param("month") int month);

    // -------------------------------------------------------------
    // 2. All-Time Query (Already correct, but verify columns match)
    // -------------------------------------------------------------
    @Query(value = """
        SELECT 
            p.id AS playerId,               -- 0
            p.name AS playerName,           -- 1
            p.phone AS phone,               -- 2
            g.name AS groupName,            -- 3
            p.join_date AS joinDate,        -- 4
            
            i.id AS installmentId,          -- 5
            i.amount AS installmentAmount,  -- 6
            COALESCE(SUM(pay.amount), 0) AS totalPaid, -- 7
            i.due_date AS dueDate,          -- 8
            i.status AS status,             -- 9
            i.remaining_amount AS remaining,-- 10
            MAX(pay.paid_on) AS lastPaymentDate -- 11
            
        FROM player p
        LEFT JOIN player_group g ON p.group_id = g.id
        JOIN installment i ON i.player_id = p.id 
        LEFT JOIN payment pay ON pay.installment_id = i.id
        
        GROUP BY p.id, p.name, p.phone, g.name, p.join_date, 
                 i.id, i.amount, i.due_date, i.status, i.remaining_amount

        ORDER BY i.due_date DESC
        """, nativeQuery = true)
    List<Object[]> fetchAllSummary();

    // ... existing imports ...

// ... inside the interface ...

    // 3. PAGINATED Query (Used for Infinite Scroll)
    @Query(value = """
        SELECT 
            p.id AS playerId, p.name AS playerName, p.phone AS phone, g.name AS groupName, p.join_date AS joinDate,
            i.id AS installmentId, i.amount AS installmentAmount, COALESCE(SUM(pay.amount), 0) AS totalPaid,
            i.due_date AS dueDate, i.status AS status, i.remaining_amount AS remaining, MAX(pay.paid_on) AS lastPaymentDate
        FROM player p
        LEFT JOIN player_group g ON p.group_id = g.id
        JOIN installment i ON i.player_id = p.id 
        LEFT JOIN payment pay ON pay.installment_id = i.id
        GROUP BY p.id, p.name, p.phone, g.name, p.join_date, i.id, i.amount, i.due_date, i.status, i.remaining_amount
        
        -- CUSTOM SORT ORDER:
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
    // 2. Count Query (Required for Pagination Metadata)
    @Query(value = "SELECT COUNT(*) FROM installment", nativeQuery = true)
    long countAllInstallments();

    // In PlayerInstallmentSummaryRepository.java
    @Query(value = """
    SELECT 
        p.id AS playerId,
        p.name AS playerName,
        p.phone AS phone,
        g.name AS groupName,
        p.join_date AS joinDate,
        
        -- CHANGE: Use SUM instead of i.amount
        SUM(i.amount) AS totalInstallmentAmount,
        COUNT(i.id) AS installmentCount,
        
        COALESCE(SUM(pay.amount), 0) AS totalPaid,
        MAX(i.due_date) AS latestDueDate,  -- or MIN() for earliest
        STRING_AGG(i.status, ',') AS statuses,  -- or handle differently
        
        -- Calculate remaining as sum of remaining amounts
        SUM(i.remaining_amount) AS totalRemaining,
        
        MAX(pay.paid_on) AS lastPaymentDate
        
    FROM player p
    LEFT JOIN player_group g ON p.group_id = g.id
    JOIN installment i ON i.player_id = p.id 
    LEFT JOIN payment pay ON pay.installment_id = i.id
    
    -- Add WHERE clause for overdue if needed
    WHERE i.due_date < CURDATE() AND i.remaining_amount > 0
    
    GROUP BY p.id, p.name, p.phone, g.name, p.join_date
    
    ORDER BY i.due_date DESC
    """, nativeQuery = true)
    List<Object[]> fetchOverdueSummary();
}