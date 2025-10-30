package ru.rtc.warehouse.warehouse.service;

import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseCreateRequest;
import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseUpdateRequest;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

import java.util.List;

public interface WarehouseService {
	void save(WarehouseCreateRequest createRequest, Long userId);
	void update(WarehouseUpdateRequest request, Long id);
	List<WarehouseDTO> findAll();
	WarehouseDTO findById(Long id);
	WarehouseDTO findByCode(String code);
	void delete(Long id);

	List<WarehouseDTO> findByUserId(Long userId);
}