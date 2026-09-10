package com.comercio.estoque.dto;

import java.math.BigDecimal;

public record InventoryCountItemResponse(

        Long id,
        Long productId,
        String productName,
        String sku,
        String barcode,
        BigDecimal expectedQuantity,
        BigDecimal countedQuantity,
        BigDecimal differenceQuantity,
        String notes

) {
}