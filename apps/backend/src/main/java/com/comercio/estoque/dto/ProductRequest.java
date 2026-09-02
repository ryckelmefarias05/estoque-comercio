package com.comercio.estoque.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductRequest(

        @NotBlank(message = "O nome do produto é obrigatório")
        @Size(max = 160)
        String name,

        @Size(max = 80)
        String sku,

        @Size(max = 80)
        String barcode,

        @Size(max = 30)
        String unit
) {
}