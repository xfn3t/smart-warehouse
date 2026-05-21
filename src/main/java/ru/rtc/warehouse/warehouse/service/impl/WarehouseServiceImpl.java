package ru.rtc.warehouse.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.controller.dto.request.ExcludedCellDTO;
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
				locationServiceAdapter.generateLocationForWarehouse(savedWarehouse, createRequest.getExcludedCells())
		));

		warehouseEntityService.save(savedWarehouse);
	}

	@Override
	public void update(WarehouseUpdateRequest updateRequest, Long id) {
		Warehouse warehouse = warehouseEntityService.findById(id);
		update(updateRequest, warehouse);
	}

	@Override
	public void update(WarehouseUpdateRequest updateRequest, String warehouseCode) {
		Warehouse warehouse = warehouseEntityService.findByCode(warehouseCode);
		update(updateRequest, warehouse);
	}

	public void update(WarehouseUpdateRequest updateRequest, Warehouse warehouse) {

		boolean dimensionsChanged = false;
		boolean exclusionsChanged = false;

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

		// excludedCells is a replace-or-clear semantic: if null, keep current; if non-null, replace
		List<ExcludedCellDTO> newExclusions = updateRequest.getExcludedCells();
		if (newExclusions != null) {
			exclusionsChanged = true;
		}

		// Regenerate locations if dimensions or exclusions changed
		if (dimensionsChanged || exclusionsChanged) {
			List<Location> updatedLocations = locationServiceAdapter.generateLocationForWarehouse(warehouse, newExclusions);
			warehouse.setLocations(new HashSet<>(updatedLocations));
		}

		warehouseEntityService.update(warehouse);
	}

	@Override
	public List<WarehouseDTO> findAll() {
		return enrichWithExclusions(warehouseMapper.toDtoList(warehouseEntityService.findAll()));
	}

	@Override
	public WarehouseDTO findById(Long id) {
		return enrichWithExclusions(warehouseMapper.toDto(warehouseEntityService.findById(id)));
	}

	@Override
	public WarehouseDTO findByCode(String code) {
		return enrichWithExclusions(warehouseMapper.toDto(warehouseEntityService.findByCode(code)));
	}

	@Override
	public void delete(Long id) {
		warehouseEntityService.delete(id);
	}

	@Override
	public void delete(String warehouseCode) {
		warehouseEntityService.delete(warehouseCode);
	}

	@Override
	public List<WarehouseDTO> findByUserId(Long userId) {
		return enrichWithExclusions(warehouseMapper.toDtoList(warehouseEntityService.findByUserId(userId)));
	}

	/**
	 * Compute excludedCells for each DTO: all (zone,row) from 1..max that have zero locations in DB.
	 */
	private List<WarehouseDTO> enrichWithExclusions(List<WarehouseDTO> dtos) {
		for (WarehouseDTO dto : dtos) {
			enrichWithExclusions(dto);
		}
		return dtos;
	}

	private WarehouseDTO enrichWithExclusions(WarehouseDTO dto) {
		if (dto == null || dto.getId() == null) return dto;
		Warehouse wh = warehouseEntityService.findById(dto.getId());
		java.util.Set<String> existingCells = new java.util.HashSet<>();
		for (Location loc : wh.getLocations()) {
			existingCells.add(loc.getZone() + "-" + loc.getRow());
		}
		java.util.List<ru.rtc.warehouse.warehouse.controller.dto.request.ExcludedCellDTO> excluded = new java.util.ArrayList<>();
		for (int z = 1; z <= dto.getZoneMaxSize(); z++) {
			for (int r = 1; r <= dto.getRowMaxSize(); r++) {
				if (!existingCells.contains(z + "-" + r)) {
					ru.rtc.warehouse.warehouse.controller.dto.request.ExcludedCellDTO ec = new ru.rtc.warehouse.warehouse.controller.dto.request.ExcludedCellDTO();
					ec.setZone(z);
					ec.setRow(r);
					excluded.add(ec);
				}
			}
		}
		dto.setExcludedCells(excluded);
		return dto;
	}
}
