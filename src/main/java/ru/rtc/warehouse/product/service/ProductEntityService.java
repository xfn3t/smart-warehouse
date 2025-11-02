package ru.rtc.warehouse.product.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.product.model.Product;

public interface ProductEntityService extends CrudEntityService<Product, Long> {
	Product findByCode(String code);

//	Integer getProductsCountByWarehouse(Long id);
}
