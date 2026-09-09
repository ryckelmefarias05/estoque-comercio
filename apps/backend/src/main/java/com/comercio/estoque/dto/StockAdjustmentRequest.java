package com.comercio.estoque.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;



public record StockAdjustmentRequest (
    @NotNull(message = "A quantidade  do ajuste é obrigatória")
    BigDecimal adjustment


){
    
}
    
    

