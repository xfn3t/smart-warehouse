package ru.rtc.warehouse.inventory.service.csv.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.rtc.warehouse.exception.InventoryImportException;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryCsvDto;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.InventoryHistoryStatusService;
import ru.rtc.warehouse.inventory.service.adapter.IHProductEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.adapter.IHWarehouseEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.csv.CsvProcessingService;
import ru.rtc.warehouse.inventory.service.csv.InventoryImportService;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.model.ProductWarehouse;
import ru.rtc.warehouse.product.service.ProductWarehouseEntityService;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.user.service.UserEntityService;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvInventoryImportServiceImpl implements InventoryImportService {

    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final IHWarehouseEntServiceAdapter warehouseService;
    private final IHProductEntServiceAdapter productService;
    private final ProductWarehouseEntityService productWarehouseEntityService;
    private final InventoryHistoryStatusService inventoryHistoryStatusService;
    private final CsvProcessingService csvProcessingService;
    private final UserEntityService userEntityService;

    @Override
    @Transactional
    public void importInventoryFromCsv(
        MultipartFile file,
        String warehouseCode
    ) {
        log.info("Импорт инвентаря из CSV для склада: {}", warehouseCode);

        User currentUser = userEntityService.getCurrentUser();
        Warehouse warehouse = warehouseService.validateAndGetWarehouse(
            warehouseCode
        );
        List<InventoryCsvDto> csvRecords = csvProcessingService.parseCsvFile(
            file
        );

        List<String> errors = new ArrayList<>();

        for (int i = 0; i < csvRecords.size(); i++) {
            InventoryCsvDto record = csvRecords.get(i);
            try {
                processInventoryRecord(record, warehouse, currentUser);
            } catch (Exception e) {
                String errorMsg = String.format(
                    "Строка %d (товар: %s): %s",
                    i + 2,
                    record.getName() != null
                        ? record.getName()
                        : (record.getSkuCode() != null
                              ? "SKU " + record.getSkuCode()
                              : "неизвестно"),
                    e.getMessage()
                );
                errors.add(errorMsg);
                log.error("Ошибка обработки записи инвентаря: {}", record, e);
            }
        }

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Обнаружены ошибки при импорте CSV:\n");
            for (String err : errors) {
                sb.append("  - ").append(err).append("\n");
            }
            throw new InventoryImportException(sb.toString().trim());
        }

        log.info(
            "Успешно импортировано {} позиций инвентаря для склада: {}",
            csvRecords.size(),
            warehouseCode
        );
    }

    private void processInventoryRecord(
        InventoryCsvDto record,
        Warehouse warehouse,
        User user
    ) {
        validateInventoryRecord(record);
        validateLocationBounds(
            record.getZone(),
            record.getRow(),
            record.getShelf(),
            warehouse
        );

        Product product = findOrCreateProduct(record, user);
        ProductWarehouse productWarehouse = findOrCreateProductWarehouse(
            product,
            warehouse,
            record
        );

        InventoryHistoryStatus inventoryHistoryStatus =
            calculateInventoryStatus(
                record.getQuantity(),
                productWarehouse.getMinStock(),
                productWarehouse.getOptimalStock()
            );

        InventoryHistory inventoryHistory = createInventoryHistory(
            record,
            warehouse,
            product,
            inventoryHistoryStatus
        );
        inventoryHistoryRepository.save(inventoryHistory);
    }

    // ── Продукт ──

    private Product findOrCreateProduct(InventoryCsvDto record, User user) {
        String csvSku = record.getSkuCode();
        boolean hasSku = csvSku != null && !csvSku.trim().isEmpty();
        boolean hasName =
            record.getName() != null && !record.getName().trim().isEmpty();

        if (hasSku) {
            String sku = csvSku.trim();
            try {
                Product existingProduct = productService.findByUserIdAndSkuCode(
                    user.getId(),
                    sku
                );

                // Проверка названия (если указано в CSV)
                if (hasName) {
                    String csvName = record.getName().trim();
                    if (!existingProduct.getName().equalsIgnoreCase(csvName)) {
                        throw new InventoryImportException(
                            String.format(
                                "Существует товар с SKU '%s', но с другим названием: '%s' (ожидалось '%s'). Проверьте SKU код.",
                                sku,
                                existingProduct.getName(),
                                csvName
                            )
                        );
                    }
                }
                if (
                    record.getCategory() != null &&
                    !record.getCategory().trim().isEmpty()
                ) {
                    existingProduct.setCategory(record.getCategory().trim());
                    productService.save(existingProduct);
                }
                return existingProduct;
            } catch (NotFoundException e) {
                if (!hasName) {
                    throw new InventoryImportException(
                        "Нельзя создать товар с новым SKU '" +
                            sku +
                            "' без указания названия (name)."
                    );
                }
                return createNewProduct(
                    user,
                    sku,
                    record.getName().trim(),
                    record.getCategory()
                );
            }
        } else {
            if (!hasName) {
                throw new InventoryImportException(
                    "Нельзя создать товар без SKU и без названия (name)."
                );
            }
            return createNewProduct(
                user,
                generateSkuCode(),
                record.getName().trim(),
                record.getCategory()
            );
        }
    }

    private Product createNewProduct(
        User user,
        String skuCode,
        String name,
        String category
    ) {
        Product newProduct = Product.builder()
            .user(user)
            .skuCode(skuCode)
            .name(name)
            .category(
                category != null && !category.trim().isEmpty()
                    ? category.trim()
                    : null
            )
            .isDeleted(false)
            .build();
        return productService.save(newProduct);
    }

    // ── Связь продукт-склад ──

    private ProductWarehouse findOrCreateProductWarehouse(
        Product product,
        Warehouse warehouse,
        InventoryCsvDto record
    ) {
        try {
            ProductWarehouse productWarehouse =
                productWarehouseEntityService.findActiveByProductAndWarehouse(
                    product.getId(),
                    warehouse.getId()
                );

            // Обновляем параметры, если переданы
            boolean updated = false;
            if (
                record.getMinStock() != null &&
                !record.getMinStock().equals(productWarehouse.getMinStock())
            ) {
                productWarehouse.setMinStock(record.getMinStock());
                updated = true;
            }
            if (
                record.getOptimalStock() != null &&
                !record
                    .getOptimalStock()
                    .equals(productWarehouse.getOptimalStock())
            ) {
                productWarehouse.setOptimalStock(record.getOptimalStock());
                updated = true;
            }
            return updated
                ? productWarehouseEntityService.update(productWarehouse)
                : productWarehouse;
        } catch (NotFoundException e) {
            // Создаем новую связь продукт-склад
            ProductWarehouse newProductWarehouse = ProductWarehouse.builder()
                .product(product)
                .warehouse(warehouse)
                .minStock(
                    record.getMinStock() != null ? record.getMinStock() : 0
                )
                .optimalStock(
                    record.getOptimalStock() != null
                        ? record.getOptimalStock()
                        : 0
                )
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
            return productWarehouseEntityService.save(newProductWarehouse);
        }
    }

    // ── Утилиты ──

    public static String generateSkuCode() {
        return UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 10)
            .toUpperCase();
    }

    private InventoryHistoryStatus calculateInventoryStatus(
        Integer quantity,
        Integer minStock,
        Integer optimalStock
    ) {
        if (quantity <= minStock) {
            return inventoryHistoryStatusService.findByCode(
                InventoryHistoryStatus.InventoryHistoryStatusCode.CRITICAL
            );
        }
        if (quantity < optimalStock) {
            return inventoryHistoryStatusService.findByCode(
                InventoryHistoryStatus.InventoryHistoryStatusCode.LOW_STOCK
            );
        }
        return inventoryHistoryStatusService.findByCode(
            InventoryHistoryStatus.InventoryHistoryStatusCode.OK
        );
    }

    private void validateInventoryRecord(InventoryCsvDto record) {
        if (record.getQuantity() == null || record.getQuantity() < 0) {
            throw new InventoryImportException(
                "Количество не может быть отрицательным или пустым"
            );
        }
        if (
            record.getZone() == null ||
            record.getRow() == null ||
            record.getShelf() == null
        ) {
            throw new InventoryImportException("Неверный формат локации");
        }
    }

    private void validateLocationBounds(
        Integer zone,
        Integer row,
        Integer shelf,
        Warehouse warehouse
    ) {
        if (
            zone < 0 || zone > warehouse.getZoneMaxSize()
        ) throw new InventoryImportException(
            "Зона вне границ склада " + warehouse.getCode()
        );
        if (
            row < 0 || row > warehouse.getRowMaxSize()
        ) throw new InventoryImportException(
            "Ряд вне границ склада " + warehouse.getCode()
        );
        if (
            shelf < 0 || shelf > warehouse.getShelfMaxSize()
        ) throw new InventoryImportException(
            "Полка вне границ склада " + warehouse.getCode()
        );
    }

    private InventoryHistory createInventoryHistory(
        InventoryCsvDto record,
        Warehouse warehouse,
        Product product,
        InventoryHistoryStatus status
    ) {
        return InventoryHistory.builder()
            .messageId(UUID.randomUUID())
            .warehouse(warehouse)
            .zone(record.getZone())
            .row(record.getRow())
            .shelf(record.getShelf())
            .product(product)
            .quantity(record.getQuantity())
            .expectedQuantity(record.getQuantity())
            .difference(0)
            .status(status)
            .scannedAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .isDeleted(false)
            .build();
    }
}
