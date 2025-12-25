package com.pca.repository;

import com.pca.model.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {

    Optional<GroupEntity> findByNameIgnoreCase(String name);

    // FIX: Add this direct delete method
    @Modifying
    @Transactional
    @Query("DELETE FROM GroupEntity g WHERE g.id = :id")
    void deleteGroupCustom(@Param("id") Long id);
}
