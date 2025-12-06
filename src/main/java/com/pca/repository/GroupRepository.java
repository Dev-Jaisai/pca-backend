package com.pca.repository;

import com.pca.model.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {
    Optional<GroupEntity> findByNameIgnoreCase(String name);
}
