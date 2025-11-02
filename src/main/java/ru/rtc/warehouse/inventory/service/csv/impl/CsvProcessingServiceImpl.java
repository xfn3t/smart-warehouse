package ru.rtc.warehouse.inventory.service.csv.impl;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameTranslateMappingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryCsvDto;
import ru.rtc.warehouse.inventory.service.csv.CsvProcessingService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CsvProcessingServiceImpl implements CsvProcessingService {

	@Override
	public List<InventoryCsvDto> parseCsvFile(MultipartFile file) {
		try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

			HeaderColumnNameTranslateMappingStrategy<InventoryCsvDto> strategy =
					new HeaderColumnNameTranslateMappingStrategy<>();
			strategy.setType(InventoryCsvDto.class);

			// Обновленный маппинг - убрали sku_code
			Map<String, String> columnMapping = new HashMap<>();
			columnMapping.put("name", "name");
			columnMapping.put("category", "category");
			columnMapping.put("location", "location");
			columnMapping.put("quantity", "quantity");
			columnMapping.put("minStock", "minStock");
			columnMapping.put("optimalStock", "optimalStock");
			strategy.setColumnMapping(columnMapping);

			List<InventoryCsvDto> result = new CsvToBeanBuilder<InventoryCsvDto>(reader)
					.withMappingStrategy(strategy)
					.withIgnoreLeadingWhiteSpace(true)
					.withSeparator(';')
					.withSkipLines(0)
					.build()
					.parse();

			// Обрабатываем location для каждого DTO
			return result.stream()
					.map(this::processLocation)
					.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("Error processing CSV file", e);
			throw new RuntimeException("Failed to process CSV file: " + e.getMessage(), e);
		}
	}

	private InventoryCsvDto processLocation(InventoryCsvDto dto) {
		// Парсим location в формат "1-1-1" на zone, row, shelf
		String location = dto.getLocation();
		if (location != null && !location.isEmpty()) {
			try {
				String[] parts = location.split("-");
				if (parts.length == 3) {
					dto.setZone(Integer.parseInt(parts[0].trim()));
					dto.setRow(Integer.parseInt(parts[1].trim()));
					dto.setShelf(Integer.parseInt(parts[2].trim()));
				} else {
					log.warn("Invalid location format: {}, expected format: zone-row-shelf", location);
				}
			} catch (NumberFormatException e) {
				log.warn("Failed to parse location: {}", location, e);
			}
		}
		return dto;
	}
}