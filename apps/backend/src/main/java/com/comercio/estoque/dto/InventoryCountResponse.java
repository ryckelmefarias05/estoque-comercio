package com.comercio.estoque.dto;

import com.comercio.estoque.entity.InventoryCountStatus;

import java.time.LocalDateTime;
import java.util.List;

public record InventoryCountResponse(

        Long id,
        Long assignedUserId,
        InventoryCountStatus status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<InventoryCountItemResponse> items

) {
}