package com.comercio.estoque.controller;

import com.comercio.estoque.dto.InventoryCountCreateRequest;
import com.comercio.estoque.dto.InventoryCountItemRequest;
import com.comercio.estoque.dto.InventoryCountResponse;
import com.comercio.estoque.service.InventoryCountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-counts")
public class InventoryCountController {

    private final InventoryCountService inventoryCountService;

    public InventoryCountController(
            InventoryCountService inventoryCountService
    ) {
        this.inventoryCountService = inventoryCountService;
    }

    public record Assignment(@jakarta.validation.constraints.NotNull Long assignedUserId) {}
    @PatchMapping("/{id}/assignment")
    public InventoryCountResponse assign(@PathVariable Long id, @Valid @RequestBody Assignment request) {
        return inventoryCountService.assign(id, request.assignedUserId());
    }

    @PostMapping
    public ResponseEntity<InventoryCountResponse> create(
            @Valid @RequestBody InventoryCountCreateRequest request
    ) {
        InventoryCountResponse response =
                inventoryCountService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<InventoryCountResponse>> findAll() {
        return ResponseEntity.ok(
                inventoryCountService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryCountResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                inventoryCountService.findById(id)
        );
    }

    @PatchMapping("/{inventoryCountId}/items/{itemId}")
    public ResponseEntity<InventoryCountResponse> countItem(
            @PathVariable Long inventoryCountId,
            @PathVariable Long itemId,
            @Valid @RequestBody InventoryCountItemRequest request
    ) {
        return ResponseEntity.ok(
                inventoryCountService.countItem(
                        inventoryCountId,
                        itemId,
                        request
                )
        );
    }

    @PatchMapping("/{inventoryCountId}/finish")
    public ResponseEntity<InventoryCountResponse> finish(
            @PathVariable Long inventoryCountId
    ) {
        return ResponseEntity.ok(
                inventoryCountService.finish(
                        inventoryCountId
                )
        );
    }
}