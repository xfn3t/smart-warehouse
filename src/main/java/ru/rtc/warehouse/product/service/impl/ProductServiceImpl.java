package ru.rtc.warehouse.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.product.controller.dto.request.ProductCreateRequest;
import ru.rtc.warehouse.product.controller.dto.request.ProductUpdateRequest;
import ru.rtc.warehouse.product.mapper.ProductMapper;
import ru.rtc.warehouse.product.mapper.ProductWarehouseMapper;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.model.ProductWarehouse;
import ru.rtc.warehouse.product.service.ProductEntityService;
import ru.rtc.warehouse.product.service.ProductService;
import ru.rtc.warehouse.product.service.ProductWarehouseEntityService;
import ru.rtc.warehouse.product.service.dto.ProductDTO;
import ru.rtc.warehouse.product.service.dto.ProductWarehouseDTO;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductEntityService productEntityService;
	private final WarehouseEntityService warehouseEntityService;
	private final ProductWarehouseEntityService productWarehouseEntityService;
	private final ProductMapper productMapper;
	private final ProductWarehouseMapper productWarehouseMapper;

	@Override
	@Transactional
	public ProductDTO create(String warehouseCode, ProductCreateRequest productCreateRequest) {
		// Генерируем SKU код
		String skuCode = generateSkuCode();

		// Создаем продукт
		Product product = productMapper.toEntity(productCreateRequest);
		product.setSkuCode(skuCode);
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
	public ProductDTO update(ProductUpdateRequest updateRequest, String productCode) {
		Product product = productEntityService.findByCode(productCode);

		if (updateRequest.getName() != null) product.setName(updateRequest.getName());
		if (updateRequest.getCategory() != null) product.setCategory(updateRequest.getCategory());

		Product updatedProduct = productEntityService.update(product);
		return enrichProductWithWarehouseInfo(updatedProduct);
	}

	@Override
	public ProductDTO findByCode(String productCode) {
		Product product = productEntityService.findByCode(productCode);
		return enrichProductWithWarehouseInfo(product);
	}

	@Override
	public List<ProductDTO> findAll() {
		List<Product> products = productEntityService.findAllActiveProducts();
		return products.stream()
				.map(this::enrichProductWithWarehouseInfo)
				.toList();
	}

	@Override
	@Transactional
	public void delete(String productCode) {
		Product product = productEntityService.findByCode(productCode);
		product.setIsDeleted(true);
		productEntityService.update(product);
	}

	private ProductDTO enrichProductWithWarehouseInfo(Product product) {
		ProductDTO productDTO = productMapper.toDto(product);

		// Получаем параметры складов для продукта
		List<ProductWarehouse> warehouseParams = productWarehouseEntityService.findActiveByProductId(product.getId());
		List<ProductWarehouseDTO> warehouseDTOs = productWarehouseMapper.toDtoList(warehouseParams);
		productDTO.setWarehouseParameters(warehouseDTOs);

		return productDTO;
	}

	private String generateSkuCode() {
		// Генерируем SKU-0001, SKU-0002 и т.д.
		Long productCount = productEntityService.count();
		return String.format("SKU-%04d", productCount + 1);
	}
}