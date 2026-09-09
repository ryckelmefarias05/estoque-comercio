package com.comercio.estoque.controller;

import com.comercio.estoque.dto.StockAdjustmentRequest;
import com.comercio.estoque.dto.StockResponse;
import com.comercio.estoque.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public ResponseEntity<List<StockResponse>> findAll() {

        return ResponseEntity.ok(
                stockService.findAll()
        );
    }

    @GetMapping("/{productId}")
    public ResponseEntity<StockResponse> findByProductId(
            @PathVariable Long productId
    ) {

        return ResponseEntity.ok(
                stockService.findByProductId(productId)
        );
    }

    @PatchMapping("/{productId}/adjustment")
    public ResponseEntity<StockResponse> adjust(
            @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {

        return ResponseEntity.ok(
                stockService.adjust(productId, request)
        );
    }
}
