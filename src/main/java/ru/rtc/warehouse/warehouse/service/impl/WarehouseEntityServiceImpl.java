package ru.rtc.warehouse.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.repository.WarehouseRepository;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseEntityServiceImpl implements WarehouseEntityService {

	private final WarehouseRepository warehouseRepository;

	@Override
	public Warehouse save(Warehouse warehouse) {
		return warehouseRepository.save(warehouse);
	}

	@Override
	public Warehouse update(Warehouse warehouse) {
		return warehouseRepository.save(warehouse);
	}

	@Override
	public List<Warehouse> findAll() {
		return warehouseRepository.findAll();
	}

	@Override
	public Warehouse findById(Long id) {
		return warehouseRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Warehouse not found"));
	}

	@Override
	public Warehouse findByCode(String code) {
		return warehouseRepository.findByCode(code)
				.orElseThrow(() -> new NotFoundException("Warehouse not found"));
	}

	@Override
	public List<Warehouse> findByUserId(Long id) {
		return warehouseRepository.findByUserId(id);
	}


	@Override
	public void delete(Long id) {
		Warehouse warehouse = findById(id);
		warehouse.setDeleted(true);
		save(warehouse);
	}

	@Override
	@Transactional(readOnly = true)
	public Warehouse validateAndGetWarehouse(String warehouseCode) {
		return warehouseRepository.findByCodeAndIsDeletedFalse(warehouseCode)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Склад не найден: " + warehouseCode
				));
	}

	@Override
	public void delete(String warehouseCode) {
		Warehouse warehouse = findByCode(warehouseCode);
		warehouse.setDeleted(true);
		save(warehouse);
	}


	@Transactional(readOnly = true)
	public List<Warehouse> findAllActiveWarehouses() {
		log.info("Finding all active warehouses");

		try {
			return warehouseRepository.findAllActiveWarehouses();
		} catch (Exception e) {
			log.error("Error finding all active warehouses", e);
			throw new RuntimeException("Failed to retrieve active warehouses", e);
		}
	}

	@Transactional(readOnly = true)
	public boolean existsByCode(String code) {
		log.info("Checking if warehouse exists by code: {}", code);

		try {
			return warehouseRepository.findByCodeAndIsDeletedFalse(code).isPresent();
		} catch (Exception e) {
			log.error("Error checking warehouse existence by code: {}", code, e);
			throw new RuntimeException("Failed to check warehouse existence", e);
		}
	}


	@Transactional
	public void softDelete(Long warehouseId) {
		log.info("Soft deleting warehouse with ID: {}", warehouseId);

		try {
			Warehouse warehouse = findById(warehouseId);
			warehouse.setDeleted(true);
			warehouseRepository.save(warehouse);
			log.info("Warehouse soft deleted with ID: {}", warehouseId);
		} catch (Exception e) {
			log.error("Error soft deleting warehouse with ID: {}", warehouseId, e);
			throw new RuntimeException("Failed to soft delete warehouse", e);
		}
	}
}