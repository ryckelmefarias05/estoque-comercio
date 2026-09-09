package com.comercio.estoque.repository;

import com.comercio.estoque.entity.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockItemRepository 
        extends JpaRepository<StockItem, Long> {
    
    Optional<StockItem> findByProductId(Long productId);
}


