package ru.rtc.warehouse.inventory.service.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.service.ProductEntityService;

@Service
@RequiredArgsConstructor
public class IHProductEntServiceAdapter {

    private final ProductEntityService productEntityService;

    public Product findByUserIdAndSkuCode(Long userId, String code) {
        return productEntityService.findByUserIdAndSkuCode(userId, code);
    }

    /**
     * Поиск продукта по userId + SKU + коду склада.
     * Основной метод для пользовательских запросов.
     */
    public Product findByUserIdAndSkuCodeAndWarehouseCode(
        Long userId,
        String skuCode,
        String warehouseCode
    ) {
        return productEntityService.findByUserIdAndSkuCodeAndWarehouseCode(
            userId,
            skuCode,
            warehouseCode
        );
    }

    /**
     * Поиск продукта по SKU + коду склада (без userId).
     * Для AI/роботных сценариев.
     */
    public Product findBySkuCodeAndWarehouseCode(
        String skuCode,
        String warehouseCode
    ) {
        return productEntityService.findAnyBySkuCodeAndWarehouseCode(
            skuCode,
            warehouseCode
        );
    }

    /**
     * Поиск продукта по SKU (глобальный, без userId).
     * Используется в MapStruct-мапперах, где нет контекста пользователя.
     * @deprecated Используйте findByUserIdAndSkuCodeAndWarehouseCode или findBySkuCodeAndWarehouseCode
     */
    @Deprecated
    public Product findByCode(String code) {
        return productEntityService.findAnyBySkuCode(code);
    }

    public Product save(Product product) {
        return productEntityService.save(product);
    }

    public Product findByNameAndCategory(String name, String category) {
        return productEntityService.findByNameAndCategory(name, category);
    }
}
