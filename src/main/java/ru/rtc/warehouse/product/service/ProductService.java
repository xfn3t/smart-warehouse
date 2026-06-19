package ru.rtc.warehouse.product.service;

import java.util.List;
import ru.rtc.warehouse.product.controller.dto.request.ProductCreateRequest;
import ru.rtc.warehouse.product.controller.dto.request.ProductUpdateRequest;
import ru.rtc.warehouse.product.controller.dto.response.UserProductOnWarehouseDTO;
import ru.rtc.warehouse.product.service.dto.ProductDTO;

public interface ProductService {
    ProductDTO create(
        String warehouseCode,
        ProductCreateRequest productCreateRequest
    );
    ProductDTO update(ProductUpdateRequest updateRequest, String productCode);
    ProductDTO findByCode(String productCode);
    List<ProductDTO> findAll();

    /**
     * Удаляет товар со склада (связь ProductWarehouse), а не сам продукт.
     */
    void delete(String productCode, String warehouseCode);

    List<UserProductOnWarehouseDTO> findUserProductsOnWarehouses();
}
