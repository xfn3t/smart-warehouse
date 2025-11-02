package ru.rtc.warehouse.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseCreateRequest;
import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseUpdateRequest;
import ru.rtc.warehouse.warehouse.mapper.WarehouseMapper;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.LocationServiceAdapter;
import ru.rtc.warehouse.warehouse.service.UserServiceAdapter;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;
import ru.rtc.warehouse.warehouse.service.WarehouseService;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

	private final LocationServiceAdapter locationServiceAdapter;
	private final UserServiceAdapter userServiceAdapter;
	private final WarehouseMapper warehouseMapper;
	private final WarehouseEntityService warehouseEntityService;

	@Override
	@Transactional
	public void save(WarehouseCreateRequest createRequest, Long userId) {
		Warehouse warehouse = warehouseMapper.toEntity(createRequest);
		warehouse.setUsers(new HashSet<>(Set.of(userServiceAdapter.getUserById(userId))));

		Warehouse savedWarehouse = warehouseEntityService.save(warehouse);

		savedWarehouse.setLocations(new HashSet<>(
				locationServiceAdapter.generateLocationForWarehouse(savedWarehouse)
		));


		warehouseEntityService.save(savedWarehouse);
	}

	@Override
	public void update(WarehouseUpdateRequest updateRequest, Long id) {
		Warehouse warehouse = warehouseEntityService.findById(id);

		boolean dimensionsChanged = false;

		if (updateRequest.getCode() != null) {
			warehouse.setCode(updateRequest.getCode());
		}
		if (updateRequest.getName() != null) {
			warehouse.setName(updateRequest.getName());
		}
		if (updateRequest.getZoneMaxSize() != null && !updateRequest.getZoneMaxSize().equals(warehouse.getZoneMaxSize())) {
			warehouse.setZoneMaxSize(updateRequest.getZoneMaxSize());
			dimensionsChanged = true;
		}
		if (updateRequest.getRowMaxSize() != null && !updateRequest.getRowMaxSize().equals(warehouse.getRowMaxSize())) {
			warehouse.setRowMaxSize(updateRequest.getRowMaxSize());
			dimensionsChanged = true;
		}
		if (updateRequest.getShelfMaxSize() != null && !updateRequest.getShelfMaxSize().equals(warehouse.getShelfMaxSize())) {
			warehouse.setShelfMaxSize(updateRequest.getShelfMaxSize());
			dimensionsChanged = true;
		}
		if (updateRequest.getLocation() != null) {
			warehouse.setWarehouseLocation(updateRequest.getLocation());
		}

		// Если изменились размеры склада, перегенерируем локации
		if (dimensionsChanged) {
			List<Location> updatedLocations = locationServiceAdapter.generateLocationForWarehouse(warehouse);
			warehouse.setLocations(new HashSet<>(updatedLocations));
		}

		warehouseEntityService.update(warehouse);
	}
	@Override
	public List<WarehouseDTO> findAll() {
		return warehouseMapper.toDtoList(warehouseEntityService.findAll());
	}

	@Override
	public WarehouseDTO findById(Long id) {
		return warehouseMapper.toDto(warehouseEntityService.findById(id));
	}

	@Override
	public WarehouseDTO findByCode(String code) {
		return warehouseMapper.toDto(warehouseEntityService.findByCode(code));
	}

	@Override
	public void delete(Long id) {
		warehouseEntityService.delete(id);
	}

	@Override
	public List<WarehouseDTO> findByUserId(Long userId) {
		return warehouseMapper.toDtoList(warehouseEntityService.findByUserId(userId));
	}
}