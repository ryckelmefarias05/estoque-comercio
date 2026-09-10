package com.comercio.estoque.repository;

import com.comercio.estoque.entity.InventoryCountItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryCountItemRepository
        extends JpaRepository<InventoryCountItem, Long> {

    List<InventoryCountItem> findByInventoryCountId(
            Long inventoryCountId
    );

    Optional<InventoryCountItem>
    findByIdAndInventoryCountId(
            Long id,
            Long inventoryCountId
    );
}