package com.comercio.estoque.dto;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        String barcode,
        String unit,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}