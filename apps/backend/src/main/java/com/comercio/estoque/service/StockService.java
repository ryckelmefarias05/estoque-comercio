package com.comercio.estoque.service;

import com.comercio.estoque.dto.StockAdjustmentRequest;
import com.comercio.estoque.dto.StockResponse;
import com.comercio.estoque.entity.Product;
import com.comercio.estoque.entity.StockItem;
import com.comercio.estoque.exception.BusinessException;
import com.comercio.estoque.exception.ResourceNotFoundException;
import com.comercio.estoque.repository.ProductRepository;
import com.comercio.estoque.repository.StockItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StockService {

    private final StockItemRepository stockItemRepository;
    private final ProductRepository productRepository;

    public StockService(
            StockItemRepository stockItemRepository,
            ProductRepository productRepository
    ) {
        this.stockItemRepository = stockItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<StockResponse> findAll() {

        return stockItemRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StockResponse findByProductId(Long productId) {

        Product product = findProductById(productId);

        return stockItemRepository.findByProductId(productId)
                .map(this::toResponse)
                .orElseGet(() -> emptyStockResponse(product));
    }

    @Transactional
    public StockResponse adjust(
            Long productId,
            StockAdjustmentRequest request
    ) {

        Product product = findProductById(productId);

        BigDecimal adjustment = request.adjustment();

        if (adjustment.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(
                    "O ajuste de estoque não pode ser zero"
            );
        }

        StockItem stockItem = stockItemRepository
                .findByProductId(productId)
                .orElseGet(() -> createEmptyStock(product));

        BigDecimal currentQuantity = stockItem.getQuantity();

        BigDecimal newQuantity =
                currentQuantity.add(adjustment);

        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "O estoque não pode ficar negativo"
            );
        }

        stockItem.setQuantity(newQuantity);

        StockItem savedStock =
                stockItemRepository.save(stockItem);

        return toResponse(savedStock);
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

    private StockItem createEmptyStock(Product product) {

        StockItem stockItem = new StockItem();

        stockItem.setProduct(product);
        stockItem.setQuantity(BigDecimal.ZERO);

        return stockItem;
    }

    private StockResponse emptyStockResponse(Product product) {

        return new StockResponse(
                null,
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getBarcode(),
                BigDecimal.ZERO,
                null
        );
    }

    private StockResponse toResponse(StockItem stockItem) {

        Product product = stockItem.getProduct();

        return new StockResponse(
                stockItem.getId(),
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getBarcode(),
                stockItem.getQuantity(),
                stockItem.getUpdatedAt()
        );
    }
}