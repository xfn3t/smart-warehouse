package ru.rtc.warehouse.inventory.service.csv.impl;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameTranslateMappingStrategy;
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

@Slf4j
@Service
public class CsvProcessingServiceImpl implements CsvProcessingService {

	@Override
	public List<InventoryCsvDto> parseCsvFile(MultipartFile file) {
		try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

			HeaderColumnNameTranslateMappingStrategy<InventoryCsvDto> strategy = getInventoryCsvDtoHeaderColumnNameTranslateMappingStrategy();

			List<InventoryCsvDto> result = new CsvToBeanBuilder<InventoryCsvDto>(reader)
					.withMappingStrategy(strategy)
					.withIgnoreLeadingWhiteSpace(true)
					.withSeparator(';')
					.withSkipLines(0)
					.build()
					.parse();

			return result.stream()
					.map(this::parseLocationFromCsv)
					.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("Ошибка обработки CSV файла", e);
			throw new RuntimeException("Failed to process CSV file: " + e.getMessage(), e);
		}
	}

	private HeaderColumnNameTranslateMappingStrategy<InventoryCsvDto> getInventoryCsvDtoHeaderColumnNameTranslateMappingStrategy() {
		HeaderColumnNameTranslateMappingStrategy<InventoryCsvDto> strategy =
				new HeaderColumnNameTranslateMappingStrategy<>();
		strategy.setType(InventoryCsvDto.class);

		Map<String, String> columnMapping = new HashMap<>();
		columnMapping.put("name", "name");
		columnMapping.put("category", "category");
		columnMapping.put("location", "location");
		columnMapping.put("quantity", "quantity");
		columnMapping.put("minStock", "minStock");
		columnMapping.put("optimalStock", "optimalStock");
		strategy.setColumnMapping(columnMapping);
		return strategy;
	}

	private InventoryCsvDto parseLocationFromCsv(InventoryCsvDto dto) {
		String location = dto.getLocation();
		if (location != null && !location.isEmpty()) {
			try {
				String[] parts = location.split("-");
				if (parts.length == 3) {
					dto.setZone(Integer.parseInt(parts[0].trim()));
					dto.setRow(Integer.parseInt(parts[1].trim()));
					dto.setShelf(Integer.parseInt(parts[2].trim()));
				} else {
					log.warn("Неверный формат локации: {}, ожидается: зона-ряд-полка", location);
					throw new IllegalArgumentException("Invalid location format: " + location);
				}
			} catch (NumberFormatException e) {
				log.warn("Ошибка парсинга локации: {}", location, e);
				throw new IllegalArgumentException("Failed to parse location: " + location);
			}
		}
		return dto;
	}
}