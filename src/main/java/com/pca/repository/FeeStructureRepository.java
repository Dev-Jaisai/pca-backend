package com.pca.repository;

import com.pca.model.FeeStructure;
import com.pca.model.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    // ... existing methods ...
    Optional<FeeStructure> findTopByGroupAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(GroupEntity group, LocalDate date);
    List<FeeStructure> findByGroupOrderByEffectiveFromDesc(GroupEntity group);
    boolean existsByGroupId(Long groupId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM fee_structure WHERE group_id = :groupId", nativeQuery = true)
    void deleteByGroupId(@Param("groupId") Long groupId);

}