package ru.rtc.warehouse.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.product.model.ProductWarehouse;
import ru.rtc.warehouse.product.repository.ProductWarehouseRepository;
import ru.rtc.warehouse.product.service.ProductWarehouseEntityService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductWarehouseEntityServiceImpl implements ProductWarehouseEntityService {

	private final ProductWarehouseRepository productWarehouseRepository;

	@Override
	public ProductWarehouse save(ProductWarehouse productWarehouse) {
		return productWarehouseRepository.save(productWarehouse);
	}

	@Override
	public ProductWarehouse update(ProductWarehouse productWarehouse) {
		return productWarehouseRepository.save(productWarehouse);
	}

	@Override
	public List<ProductWarehouse> findAll() {
		return productWarehouseRepository.findAll();
	}

	@Override
	public ProductWarehouse findById(Long id) {
		return productWarehouseRepository.findById(id).orElse(null);
	}

	@Override
	public void delete(Long id) {
		productWarehouseRepository.deleteById(id);
	}

	@Override
	public Optional<ProductWarehouse> findByProductIdAndWarehouseId(Long productId, Long warehouseId) {
		return productWarehouseRepository.findByProductIdAndWarehouseId(productId, warehouseId);
	}

	@Override
	public List<ProductWarehouse> findByProductId(Long productId) {
		return productWarehouseRepository.findByProductId(productId);
	}

	@Override
	public List<ProductWarehouse> findByWarehouseId(Long warehouseId) {
		return productWarehouseRepository.findByWarehouseId(warehouseId);
	}

	@Override
	public ProductWarehouse findActiveByProductAndWarehouse(Long productId, Long warehouseId) {
		return productWarehouseRepository.findActiveByProductAndWarehouse(productId, warehouseId)
				.orElseThrow(() -> new NotFoundException("Product warehouse not found"));
	}

	@Override
	public List<ProductWarehouse> findActiveByProductId(Long productId) {
		return productWarehouseRepository.findActiveByProductId(productId);
	}

	@Override
	public ProductWarehouse findBySkuAndWarehouseCode(String sku, String warehouseCode) {
		return productWarehouseRepository.findBySkuAndWarehouseCode(sku, warehouseCode)
				.orElseThrow(() -> new NotFoundException("Product warehouse not found"));
	}
}