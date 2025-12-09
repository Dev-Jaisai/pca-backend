package com.pca.repository;

import com.pca.model.ReminderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ReminderHistoryRepository extends JpaRepository<ReminderHistory, Long> {
    List<ReminderHistory> findByInstallmentId(Long installmentId);

    // delete by installment ids (fast, single DB statement)
    @Modifying
    @Transactional
    @Query("delete from ReminderHistory r where r.installment.id in :installmentIds")
    void deleteByInstallmentIdIn(List<Long> installmentIds);

    // optional: find blocking reminder records for debug
    @Query("select r.id from ReminderHistory r where r.installment.id in :installmentIds")
    List<Long> findIdsByInstallmentIdIn(List<Long> installmentIds);

    @Modifying
    @Transactional
    @Query("delete from ReminderHistory r where r.installment.id in :installmentIds")
    void deleteByInstallmentIds(@Param("installmentIds") List<Long> installmentIds);

    @Query("select r.installment.id from ReminderHistory r where r.installment.id in :installmentIds")
    List<Long> findRemainingInstallmentIds(@Param("installmentIds") List<Long> installmentIds);


}
