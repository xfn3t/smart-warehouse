package ru.rtc.warehouse.inventory.mapper;

import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus.InventoryHistoryStatusCode;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryStatusRepository;

@Component
@RequiredArgsConstructor
public class InventoryStatusReferenceMapper {

	private final InventoryHistoryStatusRepository repository;

	@Named("mapInventoryStatusToString")
	public String mapInventoryStatusToString(InventoryHistoryStatus status) {
		return status != null ? status.getCode().toString() : null;
	}

	@Named("mapStringToInventoryStatus")
	public InventoryHistoryStatus mapStringToInventoryStatus(String code) {
		return code != null ? repository.findByCode(InventoryHistoryStatusCode.from(code)).orElse(null) : null;
	}
}