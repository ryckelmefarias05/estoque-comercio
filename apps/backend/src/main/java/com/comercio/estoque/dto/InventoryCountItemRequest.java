package com.comercio.estoque.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InventoryCountItemRequest(

        @NotNull(
                message = "A quantidade contada é obrigatória"
        )
        @DecimalMin(
                value = "0.000",
                message = "A quantidade contada não pode ser negativa"
        )
        @jakarta.validation.constraints.Digits(integer=11, fraction=3)
        BigDecimal countedQuantity,

        @jakarta.validation.constraints.Size(max=4000) String notes

) {
}