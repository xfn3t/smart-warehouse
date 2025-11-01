package ru.rtc.warehouse.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryCsvDto;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryCreateRequest;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryUpdateRequest;
import ru.rtc.warehouse.inventory.mapper.InventoryHistoryMapper;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus.InventoryHistoryStatusCode;
import ru.rtc.warehouse.inventory.service.*;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryHistoryServiceImpl implements InventoryHistoryService {

	private final InventoryHistoryEntityService ihes;
	private final InventoryHistoryStatusService ihss;
	private final InventoryHistoryMapper ihMapper;
	private final CsvProcessingService csvService;
	private final IHWarehouseEntServiceAdapter warehouseService;
	private final IHLocationEntServiceAdapter locationService;

	private final RobotEntServiceAdapter robotAdapter;
	private final ProductEntServiceAdapter productAdapter;

	public void save(InventoryHistoryCreateRequest request) {
		InventoryHistory inventoryHistory = ihMapper.toEntity(request);
		ihes.save(inventoryHistory);
	}

	@Override
	@Transactional
	public void saveCsv(MultipartFile multipartFile, String warehouseCode) {

		List<InventoryCsvDto> inventoryCsvDtos = csvService.parseCsvFile(multipartFile);
		Warehouse warehouse = warehouseService.findByCode(warehouseCode);

		for (InventoryCsvDto dto : inventoryCsvDtos) {

			Product product = Product.builder()
					.name(dto.getName())
					.category(dto.getCategory())
					.code(dto.getSkuCode())
					.minStock(dto.getMinStock())
					.optimalStock(dto.getOptimalStock())
					.warehouse(warehouse)
					.build();

			product = productAdapter.save(product);

			InventoryHistoryStatus inventoryHistoryStatus = ihss.findByCode(getInventoryHistoryStatusCode(dto));

			ihes.save(
					InventoryHistory.builder()
							.status(inventoryHistoryStatus)
							.scannedAt(LocalDateTime.now())
							.createdAt(LocalDateTime.now())
							.warehouse(warehouse)
							.product(product)
							.quantity(dto.getQuantity())
							.expectedQuantity(dto.getQuantity())
							.difference(0)
							.location(
									locationService.findByCoordinate(
											dto.getZone(),
											dto.getRow(),
											dto.getShelf(),
											warehouse.getId()
									)
							)
							.robot(null)
					.build()
			);
		}

	}

	private InventoryHistoryStatusCode getInventoryHistoryStatusCode(InventoryCsvDto dto) {

		if (dto.getQuantity() <= dto.getMinStock()) {
			return InventoryHistoryStatusCode.CRITICAL;
		}
		if (dto.getQuantity() < dto.getOptimalStock()) {
			return InventoryHistoryStatusCode.LOW_STOCK;
		}

		return InventoryHistoryStatusCode.OK;
	}

	public void update(InventoryHistoryUpdateRequest request, Long id) {

		InventoryHistory inventoryHistory = ihes.findById(id);

		String robotCode = request.getRobotCode();
		String productCode = request.getProductCode();
		Integer quantity = request.getQuantity();
		Integer zone = request.getZone();
		Integer rowNumber = request.getRowNumber();
		Integer shelfNumber = request.getShelfNumber();
		InventoryHistoryStatusCode status = InventoryHistoryStatusCode.from(String.valueOf(request.getStatus()));
		LocalDateTime scannedAt = request.getScannedAt();

		if (robotCode != null) inventoryHistory.setRobot(robotAdapter.findByCode(robotCode));
		if (productCode != null) inventoryHistory.setProduct(productAdapter.findByCode(productCode));
		if (quantity != null) inventoryHistory.setQuantity(quantity);
		if (zone != null) inventoryHistory.getLocation().setZone(zone);
		if (rowNumber != null) inventoryHistory.getLocation().setRow(rowNumber);
		if (shelfNumber != null) inventoryHistory.getLocation().setShelf(shelfNumber);
		if (status != null) inventoryHistory.setStatus(ihss.findByCode(status));
		if (scannedAt != null) inventoryHistory.setScannedAt(scannedAt);

		ihes.update(inventoryHistory);
	}

	@Transactional(readOnly = true)
	public List<InventoryHistoryDTO> findAll() {
		return ihMapper.toDtoList(ihes.findAll());
	}

	@Transactional(readOnly = true)
	public InventoryHistoryDTO findById(Long id) {
		return ihMapper.toDto(ihes.findById(id));
	}

	public void delete(Long id) {
		ihes.delete(id);
	}

}
