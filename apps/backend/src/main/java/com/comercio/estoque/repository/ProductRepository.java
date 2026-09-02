package com.comercio.estoque.repository;

import com.comercio.estoque.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    Optional<Product> findBySku(String sku);

    boolean existsByBarcode(String barcode);

    boolean existsBySku(String sku);
}