package com.pca.repository;

import com.pca.model.ReminderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderHistoryRepository extends JpaRepository<ReminderHistory, Long> {
    List<ReminderHistory> findByInstallmentId(Long installmentId);
}
