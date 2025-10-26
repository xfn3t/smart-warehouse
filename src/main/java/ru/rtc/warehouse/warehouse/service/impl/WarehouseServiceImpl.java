package ru.rtc.warehouse.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseCreateRequest;
import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseUpdateRequest;
import ru.rtc.warehouse.warehouse.mapper.WarehouseMapper;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;
import ru.rtc.warehouse.warehouse.service.WarehouseService;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

	private final WarehouseMapper warehouseMapper;
	private final WarehouseEntityService warehouseEntityService;

	@Override
	public void save(WarehouseCreateRequest createRequest) {
		Warehouse warehouse = warehouseMapper.toEntity(convertToDTO(createRequest));
		warehouseEntityService.save(warehouse);
	}

	@Override
	public void update(WarehouseUpdateRequest updateRequest, Long id) {
		Warehouse warehouse = warehouseEntityService.findById(id);

		if (updateRequest.getCode() != null) {
			warehouse.setCode(updateRequest.getCode());
		}
		if (updateRequest.getName() != null) {
			warehouse.setName(updateRequest.getName());
		}
		if (updateRequest.getZoneMaxSize() != null) {
			warehouse.setZoneMaxSize(updateRequest.getZoneMaxSize());
		}
		if (updateRequest.getRowMaxSize() != null) {
			warehouse.setRowMaxSize(updateRequest.getRowMaxSize());
		}
		if (updateRequest.getShelfMaxSize() != null) {
			warehouse.setShelfMaxSize(updateRequest.getShelfMaxSize());
		}
		if (updateRequest.getLocation() != null) {
			warehouse.setLocation(updateRequest.getLocation());
		}

		warehouseEntityService.update(warehouse);
	}

	@Override
	public List<WarehouseDTO> findAll() {
		return warehouseEntityService.findAll().stream()
				.map(warehouseMapper::toDto)
				.toList();
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

	private WarehouseDTO convertToDTO(WarehouseCreateRequest request) {
		return WarehouseDTO.builder()
				.code(request.getCode())
				.name(request.getName())
				.zoneMaxSize(request.getZoneMaxSize())
				.rowMaxSize(request.getRowMaxSize())
				.shelfMaxSize(request.getShelfMaxSize())
				.location(request.getLocation())
				.build();
	}
}