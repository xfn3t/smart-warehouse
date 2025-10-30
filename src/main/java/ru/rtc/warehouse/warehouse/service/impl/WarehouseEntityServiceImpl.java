package ru.rtc.warehouse.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.repository.WarehouseRepository;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseEntityServiceImpl implements WarehouseEntityService {

	private final WarehouseRepository warehouseRepository;

	@Override
	public void save(Warehouse warehouse) {
		warehouseRepository.save(warehouse);
	}

	@Override
	public Warehouse saveAndReturn(Warehouse warehouse) {
		return warehouseRepository.save(warehouse);
	}

	@Override
	public void update(Warehouse warehouse) {
		warehouseRepository.save(warehouse);
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
		warehouseRepository.deleteById(id);
	}
}