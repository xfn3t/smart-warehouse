package ru.rtc.warehouse.warehouse.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import ru.rtc.warehouse.warehouse.controller.dto.request.ExcludedCellDTO;
import ru.rtc.warehouse.warehouse.controller.dto.request.WarehouseCreateRequest;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface WarehouseMapper {
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mapping(source = "warehouseLocation", target = "location")
    @Mapping(
        source = "excludedCellsJson",
        target = "excludedCells",
        qualifiedByName = "mapToExcludedCells"
    )
    WarehouseDTO toDto(Warehouse entity);

    @Mapping(target = "excludedCells", ignore = true)
    WarehouseDTO toDto(WarehouseCreateRequest warehouseCreateRequest);

    @Mapping(source = "location", target = "warehouseLocation")
    @Mapping(target = "excludedCellsJson", ignore = true)
    Warehouse toEntity(WarehouseDTO dto);

    @Mapping(source = "location", target = "warehouseLocation")
    @Mapping(target = "excludedCellsJson", ignore = true)
    Warehouse toEntity(WarehouseCreateRequest request);

    List<WarehouseDTO> toDtoList(List<Warehouse> warehouses);

    @Named("mapToExcludedCells")
    @SuppressWarnings("unchecked")
    static List<ExcludedCellDTO> mapToExcludedCells(
        Map<String, Object> jsonMap
    ) {
        if (jsonMap == null || jsonMap.isEmpty()) {
            return Collections.emptyList();
        }
        // Hibernate stores JSONB as Map; our format is [{"zone":1,"row":4},...]
        // But since the column stores a JSON array at top level, Hibernate may
        // return it as List<Map> or just Map depending on driver.
        // We handle both: if there's a key "cells" — use it; otherwise convert.
        Object cells = jsonMap.get("cells");
        if (cells instanceof List) {
            return OBJECT_MAPPER.convertValue(
                cells,
                new TypeReference<List<ExcludedCellDTO>>() {}
            );
        }
        // Fallback: the whole map is the array (shouldn't happen with array root)
        return Collections.emptyList();
    }
}
