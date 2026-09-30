package com.comercio.estoque.repository;

import com.comercio.estoque.entity.InventoryCount;
import com.comercio.estoque.entity.InventoryCountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryCountRepository
        extends JpaRepository<InventoryCount, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from InventoryCount c where c.id = :id")
    java.util.Optional<InventoryCount> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

    List<InventoryCount> findByAssignedUserId(Long id);

    List<InventoryCount> findByStatus(
            InventoryCountStatus status
    );
}