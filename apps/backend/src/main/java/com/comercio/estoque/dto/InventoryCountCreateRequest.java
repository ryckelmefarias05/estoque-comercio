package com.comercio.estoque.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InventoryCountCreateRequest(

        @NotEmpty(
                message = "Informe pelo menos um produto para a contagem"
        )
        List<Long> productIds

) {
}