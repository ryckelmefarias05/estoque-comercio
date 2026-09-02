package com.comercio.estoque.service;

import com.comercio.estoque.dto.ProductRequest;
import com.comercio.estoque.dto.ProductResponse;
import com.comercio.estoque.entity.Product;
import com.comercio.estoque.exception.BusinessException;
import com.comercio.estoque.exception.ResourceNotFoundException;
import com.comercio.estoque.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(ProductRequest request) {
        validateUniqueFields(request, null);

        Product product = new Product();

        product.setName(request.name().trim());
        product.setSku(normalize(request.sku()));
        product.setBarcode(normalize(request.barcode()));
        product.setUnit(normalizeUnit(request.unit()));

        Product savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse findById(Long id) {
        Product product = findEntityById(id);

        return toResponse(product);
    }

    public ProductResponse findByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Produto não encontrado para o código de barras informado"
                        )
                );

        return toResponse(product);
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findEntityById(id);

        validateUniqueFields(request, id);

        product.setName(request.name().trim());
        product.setSku(normalize(request.sku()));
        product.setBarcode(normalize(request.barcode()));
        product.setUnit(normalizeUnit(request.unit()));

        Product updatedProduct = productRepository.save(product);

        return toResponse(updatedProduct);
    }

    public void deactivate(Long id) {
        Product product = findEntityById(id);

        product.setActive(false);

        productRepository.save(product);
    }

    private Product findEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Produto com ID " + id + " não encontrado"
                        )
                );
    }

    private void validateUniqueFields(ProductRequest request, Long currentProductId) {

        String sku = normalize(request.sku());
        String barcode = normalize(request.barcode());

        if (sku != null) {
            productRepository.findBySku(sku)
                    .filter(product ->
                            currentProductId == null
                                    || !product.getId().equals(currentProductId)
                    )
                    .ifPresent(product -> {
                        throw new BusinessException(
                                "Já existe um produto cadastrado com este SKU"
                        );
                    });
        }

        if (barcode != null) {
            productRepository.findByBarcode(barcode)
                    .filter(product ->
                            currentProductId == null
                                    || !product.getId().equals(currentProductId)
                    )
                    .ifPresent(product -> {
                        throw new BusinessException(
                                "Já existe um produto cadastrado com este código de barras"
                        );
                    });
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return "UN";
        }

        return unit.trim().toUpperCase();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getBarcode(),
                product.getUnit(),
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}