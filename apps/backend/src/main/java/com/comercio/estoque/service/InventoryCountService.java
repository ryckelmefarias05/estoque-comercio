package com.comercio.estoque.service;

import com.comercio.estoque.dto.InventoryCountCreateRequest;
import com.comercio.estoque.dto.InventoryCountItemRequest;
import com.comercio.estoque.dto.InventoryCountItemResponse;
import com.comercio.estoque.dto.InventoryCountResponse;
import com.comercio.estoque.entity.InventoryCount;
import com.comercio.estoque.entity.InventoryCountItem;
import com.comercio.estoque.entity.InventoryCountStatus;
import com.comercio.estoque.entity.Product;
import com.comercio.estoque.entity.StockItem;
import com.comercio.estoque.exception.BusinessException;
import com.comercio.estoque.exception.ResourceNotFoundException;
import com.comercio.estoque.repository.InventoryCountItemRepository;
import com.comercio.estoque.repository.InventoryCountRepository;
import com.comercio.estoque.repository.ProductRepository;
import com.comercio.estoque.repository.StockItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class InventoryCountService {

    private final com.comercio.estoque.operation.AccessService access;
    private final InventoryCountRepository inventoryCountRepository;
    private final InventoryCountItemRepository inventoryCountItemRepository;
    private final ProductRepository productRepository;
    private final StockItemRepository stockItemRepository;

    public InventoryCountService(
            com.comercio.estoque.operation.AccessService access,
            InventoryCountRepository inventoryCountRepository,
            InventoryCountItemRepository inventoryCountItemRepository,
            ProductRepository productRepository,
            StockItemRepository stockItemRepository
    ) {
        this.access = access;
        this.inventoryCountRepository = inventoryCountRepository;
        this.inventoryCountItemRepository = inventoryCountItemRepository;
        this.productRepository = productRepository;
        this.stockItemRepository = stockItemRepository;
    }

    @Transactional
    public InventoryCountResponse create(
            InventoryCountCreateRequest request
    ) {
        access.requireAdmin();
        access.validateOperator(request.assignedUserId());
        validateDuplicatedProducts(request.productIds());

        InventoryCount inventoryCount = new InventoryCount();
        inventoryCount.setAssignedUserId(request.assignedUserId());
        inventoryCount.setCreatedByUserId(access.id());

        for (Long productId : request.productIds()) {

            Product product = findProductById(productId);

            BigDecimal expectedQuantity =
                    findCurrentStockQuantity(productId);

            InventoryCountItem item =
                    new InventoryCountItem();

            item.setProduct(product);
            item.setExpectedQuantity(expectedQuantity);

            inventoryCount.addItem(item);
        }

        InventoryCount savedInventoryCount =
                inventoryCountRepository.save(inventoryCount);

        return toResponse(savedInventoryCount);
    }

    @Transactional(readOnly = true)
    public List<InventoryCountResponse> findAll() {

        return (access.admin() ? inventoryCountRepository.findAll() : inventoryCountRepository.findByAssignedUserId(access.id()))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryCountResponse findById(Long id) {

        InventoryCount inventoryCount =
                findInventoryById(id);

        return toResponse(inventoryCount);
    }

    @Transactional
    public InventoryCountResponse countItem(
            Long inventoryCountId,
            Long itemId,
            InventoryCountItemRequest request
    ) {
        InventoryCount inventoryCount =
                findInventoryForUpdate(inventoryCountId);

        validateInventoryCanBeChanged(inventoryCount);

        InventoryCountItem item =
                inventoryCountItemRepository
                        .findByIdAndInventoryCountId(
                                itemId,
                                inventoryCountId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Item não encontrado nesta contagem"
                                )
                        );

        if (inventoryCount.getStatus()
                == InventoryCountStatus.OPEN) {

            inventoryCount.setStatus(
                    InventoryCountStatus.IN_PROGRESS
            );

            inventoryCount.setStartedAt(
                    LocalDateTime.now()
            );
        }

        BigDecimal countedQuantity =
                request.countedQuantity();

        BigDecimal difference =
                countedQuantity.subtract(
                        item.getExpectedQuantity()
                );

        item.setCountedQuantity(countedQuantity);
        item.setDifferenceQuantity(difference);
        item.setNotes(request.notes());

        inventoryCountRepository.save(inventoryCount);
        inventoryCountItemRepository.save(item);

        return toResponse(inventoryCount);
    }

    @Transactional
    public InventoryCountResponse finish(
            Long inventoryCountId
    ) {
        InventoryCount inventoryCount =
                findInventoryForUpdate(inventoryCountId);

        validateInventoryCanBeChanged(inventoryCount);

        boolean hasPendingItems =
                inventoryCount.getItems()
                        .stream()
                        .anyMatch(item ->
                                item.getCountedQuantity() == null
                        );

        if (hasPendingItems) {
            throw new BusinessException(
                    "Não é possível finalizar a contagem enquanto houver itens pendentes"
            );
        }

        inventoryCount.setStatus(
                InventoryCountStatus.FINISHED
        );

        inventoryCount.setFinishedAt(
                LocalDateTime.now()
        );

        InventoryCount savedInventoryCount =
                inventoryCountRepository.save(inventoryCount);

        return toResponse(savedInventoryCount);
    }

    @Transactional
    public InventoryCountResponse assign(Long id, Long operatorId) {
        access.requireAdmin();
        access.validateOperator(operatorId);
        InventoryCount count = findInventoryForUpdate(id);
        validateInventoryCanBeChanged(count);
        count.setAssignedUserId(operatorId);
        return toResponse(inventoryCountRepository.save(count));
    }

    private void validateDuplicatedProducts(
            List<Long> productIds
    ) {
        Set<Long> uniqueIds =
                new HashSet<>(productIds);

        if (uniqueIds.size() != productIds.size()) {
            throw new BusinessException(
                    "A contagem não pode conter produtos duplicados"
            );
        }
    }

    private void validateInventoryCanBeChanged(
            InventoryCount inventoryCount
    ) {
        if (inventoryCount.getStatus()
                == InventoryCountStatus.FINISHED) {

            throw new BusinessException(
                    "Esta contagem já foi finalizada"
            );
        }
    }

    private Product findProductById(Long productId) {

        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Produto com ID "
                                        + productId
                                        + " não encontrado"
                        )
                );
    }

    private InventoryCount findInventoryById(
            Long inventoryCountId
    ) {
        InventoryCount count = inventoryCountRepository
                .findById(inventoryCountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Contagem com ID "
                                        + inventoryCountId
                                        + " não encontrada"
                        )
                );
        access.checkOwner(count.getAssignedUserId());
        return count;
    }

    private InventoryCount findInventoryForUpdate(Long id) {
        InventoryCount count = inventoryCountRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contagem não encontrada"));
        access.checkOwner(count.getAssignedUserId());
        return count;
    }

    private BigDecimal findCurrentStockQuantity(
            Long productId
    ) {
        return stockItemRepository
                .findByProductId(productId)
                .map(StockItem::getQuantity)
                .orElse(BigDecimal.ZERO);
    }

    private InventoryCountResponse toResponse(
            InventoryCount inventoryCount
    ) {
        List<InventoryCountItemResponse> items =
                inventoryCount.getItems()
                        .stream()
                        .map(this::toItemResponse)
                        .toList();

        return new InventoryCountResponse(
                inventoryCount.getId(),
                inventoryCount.getAssignedUserId(),
                inventoryCount.getStatus(),
                inventoryCount.getStartedAt(),
                inventoryCount.getFinishedAt(),
                inventoryCount.getCreatedAt(),
                inventoryCount.getUpdatedAt(),
                items
        );
    }

    private InventoryCountItemResponse toItemResponse(
            InventoryCountItem item
    ) {
        Product product = item.getProduct();

        return new InventoryCountItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getBarcode(),
                item.getExpectedQuantity(),
                item.getCountedQuantity(),
                item.getDifferenceQuantity(),
                item.getNotes()
        );
    }
}