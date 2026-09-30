package com.comercio.estoque.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InventoryCountCreateRequest(

        @NotEmpty(
                message = "Informe pelo menos um produto para a contagem"
        )
        @jakarta.validation.constraints.Size(max=5000)
        List<@jakarta.validation.constraints.NotNull Long> productIds,
        @jakarta.validation.constraints.NotNull Long assignedUserId

) {
}