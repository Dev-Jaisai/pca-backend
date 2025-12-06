package com.pca.repository;

import com.pca.model.FeeStructure;
import com.pca.model.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {
    /**
     * Returns the most recent effective FeeStructure for a group before or on the given date.
     */
    Optional<FeeStructure> findTopByGroupAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(GroupEntity group, LocalDate date);

    /**
     * Convenience to get all fee structures for a group (history).
     */
    List<FeeStructure> findByGroupOrderByEffectiveFromDesc(GroupEntity group);
}
