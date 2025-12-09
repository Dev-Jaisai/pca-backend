package com.pca.repository;

import com.pca.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInstallmentIdOrderByPaidOnDesc(Long installmentId);
    @Modifying
    @Transactional
    @Query("delete from Payment p where p.installment.player.id = :playerId")
    void deleteByPlayerId(@Param("playerId") Long playerId);

    @Modifying
    @Transactional
    @Query("delete from Payment p where p.installment.id in :installmentIds")
    void deleteByInstallmentIds(@Param("installmentIds") List<Long> installmentIds);

}
