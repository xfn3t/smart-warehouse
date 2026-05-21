package ru.rtc.warehouse.warehouse.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
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

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LocationServiceAdapter locationServiceAdapter;
    private final UserServiceAdapter userServiceAdapter;
    private final WarehouseMapper warehouseMapper;
    private final WarehouseEntityService warehouseEntityService;

    @Override
    @Transactional
    public void save(WarehouseCreateRequest createRequest, Long userId) {
        Warehouse warehouse = warehouseMapper.toEntity(createRequest);
        warehouse.setUsers(
            new HashSet<>(Set.of(userServiceAdapter.getUserById(userId)))
        );

        // Save excludedCells as JSONB via Map
        warehouse.setExcludedCellsJson(
            cellsToMap(createRequest.getExcludedCells())
        );

        Warehouse savedWarehouse = warehouseEntityService.save(warehouse);

        savedWarehouse.setLocations(
            new HashSet<>(
                locationServiceAdapter.generateLocationForWarehouse(
                    savedWarehouse,
                    createRequest.getExcludedCells()
                )
            )
        );

        warehouseEntityService.save(savedWarehouse);
    }

    @Override
    public void update(WarehouseUpdateRequest updateRequest, Long id) {
        Warehouse warehouse = warehouseEntityService.findById(id);
        update(updateRequest, warehouse);
    }

    @Override
    public void update(
        WarehouseUpdateRequest updateRequest,
        String warehouseCode
    ) {
        Warehouse warehouse = warehouseEntityService.findByCode(warehouseCode);
        update(updateRequest, warehouse);
    }

    public void update(
        WarehouseUpdateRequest updateRequest,
        Warehouse warehouse
    ) {
        boolean dimensionsChanged = false;
        boolean exclusionsChanged = false;

        if (updateRequest.getCode() != null) warehouse.setCode(
            updateRequest.getCode()
        );
        if (updateRequest.getName() != null) warehouse.setName(
            updateRequest.getName()
        );

        if (
            updateRequest.getZoneMaxSize() != null &&
            !updateRequest.getZoneMaxSize().equals(warehouse.getZoneMaxSize())
        ) {
            warehouse.setZoneMaxSize(updateRequest.getZoneMaxSize());
            dimensionsChanged = true;
        }
        if (
            updateRequest.getRowMaxSize() != null &&
            !updateRequest.getRowMaxSize().equals(warehouse.getRowMaxSize())
        ) {
            warehouse.setRowMaxSize(updateRequest.getRowMaxSize());
            dimensionsChanged = true;
        }
        if (
            updateRequest.getShelfMaxSize() != null &&
            !updateRequest.getShelfMaxSize().equals(warehouse.getShelfMaxSize())
        ) {
            warehouse.setShelfMaxSize(updateRequest.getShelfMaxSize());
            dimensionsChanged = true;
        }
        if (updateRequest.getLocation() != null) warehouse.setWarehouseLocation(
            updateRequest.getLocation()
        );

        // excludedCells: null = keep, non-null = replace
        List<ExcludedCellDTO> newExclusions = updateRequest.getExcludedCells();
        if (newExclusions != null) {
            warehouse.setExcludedCellsJson(cellsToMap(newExclusions));
            exclusionsChanged = true;
        }

        List<ExcludedCellDTO> effectiveExclusions = mapToCells(
            warehouse.getExcludedCellsJson()
        );

        if (dimensionsChanged || exclusionsChanged) {
            List<Location> updatedLocations =
                locationServiceAdapter.generateLocationForWarehouse(
                    warehouse,
                    effectiveExclusions
                );
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
    public void delete(String code) {
        warehouseEntityService.delete(code);
    }

    @Override
    public List<WarehouseDTO> findByUserId(Long id) {
        return warehouseMapper.toDtoList(
            warehouseEntityService.findByUserId(id)
        );
    }

    /** Convert List<ExcludedCellDTO> to Map<String,Object> for JSONB storage as {"cells": [...]} */
    private Map<String, Object> cellsToMap(List<ExcludedCellDTO> cells) {
        if (cells == null || cells.isEmpty()) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("cells", cells);
        return map;
    }

    /** Convert Map<String,Object> from JSONB to List<ExcludedCellDTO> */
    @SuppressWarnings("unchecked")
    private List<ExcludedCellDTO> mapToCells(Map<String, Object> jsonMap) {
        if (
            jsonMap == null || jsonMap.isEmpty()
        ) return Collections.emptyList();
        Object cells = jsonMap.get("cells");
        if (cells instanceof List) {
            return OBJECT_MAPPER.convertValue(
                cells,
                new com.fasterxml.jackson.core.type.TypeReference<
                    List<ExcludedCellDTO>
                >() {}
            );
        }
        return Collections.emptyList();
    }
}
