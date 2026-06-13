package ru.rtc.warehouse.product.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.common.aspect.RequiresOwnership;
import ru.rtc.warehouse.product.controller.dto.request.ProductCreateRequest;
import ru.rtc.warehouse.product.controller.dto.request.ProductUpdateRequest;
import ru.rtc.warehouse.product.service.ProductEntityService;
import ru.rtc.warehouse.product.service.ProductService;
import ru.rtc.warehouse.product.service.dto.ProductDTO;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductEntityService productEntityService;

    @PostMapping("/warehouses/{warehouseCode}")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<ProductDTO> create(
        @PathVariable String warehouseCode,
        @Valid @RequestBody ProductCreateRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(productService.create(warehouseCode, request));
    }

    @PutMapping("/{productCode}")
    public ResponseEntity<ProductDTO> update(
        @PathVariable String productCode,
        @Valid @RequestBody ProductUpdateRequest request
    ) {
        return ResponseEntity.ok(productService.update(request, productCode));
    }

    @GetMapping("/{productCode}")
    public ResponseEntity<ProductDTO> findByCode(
        @PathVariable String productCode
    ) {
        return ResponseEntity.ok(productService.findByCode(productCode));
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    @DeleteMapping("/{productCode}")
    public ResponseEntity<Void> delete(@PathVariable String productCode) {
        productService.delete(productCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(productEntityService.findDistinctCategories());
    }
}
