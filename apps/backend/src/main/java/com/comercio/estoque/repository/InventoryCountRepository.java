package com.comercio.estoque.repository;

import com.comercio.estoque.entity.InventoryCount;
import com.comercio.estoque.entity.InventoryCountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryCountRepository
        extends JpaRepository<InventoryCount, Long> {

    List<InventoryCount> findByStatus(
            InventoryCountStatus status
    );
}