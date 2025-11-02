package ru.rtc.warehouse.inventory.service.csv;

import org.springframework.web.multipart.MultipartFile;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryCsvDto;

import java.util.List;

public interface CsvProcessingService {
	List<InventoryCsvDto> parseCsvFile(MultipartFile file);
}
