package ru.rtc.warehouse.product.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.product.controller.dto.request.ProductCreateRequest;
import ru.rtc.warehouse.product.controller.dto.request.ProductUpdateRequest;
import ru.rtc.warehouse.product.controller.dto.response.UserProductOnWarehouseDTO;
import ru.rtc.warehouse.product.mapper.ProductMapper;
import ru.rtc.warehouse.product.mapper.ProductWarehouseMapper;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.model.ProductWarehouse;
import ru.rtc.warehouse.product.service.ProductEntityService;
import ru.rtc.warehouse.product.service.ProductService;
import ru.rtc.warehouse.product.service.ProductWarehouseEntityService;
import ru.rtc.warehouse.product.service.dto.ProductDTO;
import ru.rtc.warehouse.product.service.dto.ProductWarehouseDTO;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.user.service.UserEntityService;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductEntityService productEntityService;
    private final WarehouseEntityService warehouseEntityService;
    private final ProductWarehouseEntityService productWarehouseEntityService;
    private final ProductMapper productMapper;
    private final ProductWarehouseMapper productWarehouseMapper;
    private final UserEntityService userEntityService;

    @Override
    @Transactional
    public ProductDTO create(
        String warehouseCode,
        ProductCreateRequest productCreateRequest
    ) {
        // Получаем текущего пользователя
        User currentUser = userEntityService.getCurrentUser();

        // Генерируем SKU код
        String skuCode = generateSkuCode();

        // Создаем продукт
        Product product = productMapper.toEntity(productCreateRequest);
        product.setSkuCode(skuCode);
        product.setUser(currentUser);
        Product savedProduct = productEntityService.save(product);

        // Находим склад
        Warehouse warehouse = warehouseEntityService.findByCode(warehouseCode);

        // Создаем связь продукт-склад
        ProductWarehouse productWarehouse = ProductWarehouse.builder()
            .product(savedProduct)
            .warehouse(warehouse)
            .minStock(productCreateRequest.getMinStock())
            .optimalStock(productCreateRequest.getOptimalStock())
            .createdAt(LocalDateTime.now())
            .isDeleted(false)
            .build();

        productWarehouseEntityService.save(productWarehouse);

        return enrichProductWithWarehouseInfo(savedProduct);
    }

    @Override
    @Transactional
    public ProductDTO update(
        ProductUpdateRequest updateRequest,
        String productCode
    ) {
        User currentUser = userEntityService.getCurrentUser();
        Product product = productEntityService.findByUserIdAndSkuCode(
            currentUser.getId(),
            productCode
        );

        if (updateRequest.getName() != null) product.setName(
            updateRequest.getName()
        );
        if (updateRequest.getCategory() != null) product.setCategory(
            updateRequest.getCategory()
        );
        if (updateRequest.getDescription() != null) product.setDescription(
            updateRequest.getDescription()
        );

        Product updatedProduct = productEntityService.update(product);
        return enrichProductWithWarehouseInfo(updatedProduct);
    }

    @Override
    public ProductDTO findByCode(String productCode) {
        User currentUser = userEntityService.getCurrentUser();
        Product product = productEntityService.findByUserIdAndSkuCode(
            currentUser.getId(),
            productCode
        );
        return enrichProductWithWarehouseInfo(product);
    }

    @Override
    public List<ProductDTO> findAll() {
        List<Product> products = productEntityService.findAllActiveProducts();
        return products
            .stream()
            .map(this::enrichProductWithWarehouseInfo)
            .toList();
    }

    @Override
    @Transactional
    public void delete(String productCode) {
        User currentUser = userEntityService.getCurrentUser();
        Product product = productEntityService.findByUserIdAndSkuCode(
            currentUser.getId(),
            productCode
        );
        product.setIsDeleted(true);
        productEntityService.update(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProductOnWarehouseDTO> findUserProductsOnWarehouses() {
        User currentUser = userEntityService.getCurrentUser();

        List<Object[]> rows = productEntityService.findUserProductsOnWarehouses(
            currentUser.getId()
        );

        List<UserProductOnWarehouseDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(
                UserProductOnWarehouseDTO.builder()
                    .skuCode((String) row[0])
                    .productName((String) row[1])
                    .category((String) row[2])
                    .imageUrl((String) row[3])
                    .warehouseCode((String) row[4])
                    .warehouseName((String) row[5])
                    .quantity((Integer) row[6])
                    .zone((Integer) row[7])
                    .row((Integer) row[8])
                    .shelf((Integer) row[9])
                    .minStock((Integer) row[10])
                    .optimalStock((Integer) row[11])
                    .build()
            );
        }
        return result;
    }

    private ProductDTO enrichProductWithWarehouseInfo(Product product) {
        ProductDTO productDTO = productMapper.toDto(product);

        // Получаем параметры складов для продукта
        List<ProductWarehouse> warehouseParams =
            productWarehouseEntityService.findActiveByProductId(
                product.getId()
            );
        List<ProductWarehouseDTO> warehouseDTOs =
            productWarehouseMapper.toDtoList(warehouseParams);
        productDTO.setWarehouseParameters(warehouseDTOs);

        return productDTO;
    }

    public static String generateSkuCode() {
        // Генерируем SKU на основе UUID, 10 символов
        return UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 10)
            .toUpperCase();
    }
}
