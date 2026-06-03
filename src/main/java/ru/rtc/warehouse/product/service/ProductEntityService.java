package ru.rtc.warehouse.product.service;

import java.util.List;
import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.product.model.Product;

public interface ProductEntityService extends CrudEntityService<Product, Long> {
    Product findBySkuCode(String skuCode);
    List<Product> findAllActiveProducts();
    Long count();
    Product findByNameAndCategory(String name, String category);

    List<Product> findAllByWarehouseCode(String warehouseCode);

    List<String> findDistinctCategories();
}
