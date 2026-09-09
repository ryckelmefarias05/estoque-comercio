package com.comercio.estoque.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockResponse(

    Long stockItemId,
    Long productId,
    String productName,
    String sku,
    String barcode,
    BigDecimal quantity,
    LocalDateTime updatedAt
) {
    
}
