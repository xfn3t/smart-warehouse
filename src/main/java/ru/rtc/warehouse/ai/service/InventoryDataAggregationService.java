package ru.rtc.warehouse.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryDataAggregationService {

	private final InventoryHistoryRepository historyRepository;

	public Map<String, Object> buildFeatureSet(Long productId, int horizonDays) {

		LocalDateTime to = LocalDateTime.now();
		LocalDateTime from = to.minusDays(60);

		List<InventoryHistory> history = historyRepository.findByProductAndPeriod(productId, from, to);

		if (history.isEmpty()) {
			throw new IllegalStateException("Недостаточно данных для продукта ID=" + productId);
		}

		List<Map<String, Object>> dailyData = history.stream()
				.collect(Collectors.groupingBy(
						h -> h.getScannedAt().toLocalDate(),
						Collectors.summingInt(InventoryHistory::getQuantity)
				))
				.entrySet()
				.stream()
				.map(e -> {
					Map<String, Object> map = new HashMap<>();
					map.put("date", e.getKey().toString());
					map.put("quantity", e.getValue());
					return map;
				})
				.sorted(Comparator.comparing(m -> (String) m.get("date")))
				.toList();

		Map<String, Object> featureSet = new HashMap<>();
		featureSet.put("product_id", productId);
		featureSet.put("horizon_days", horizonDays);
		featureSet.put("history", dailyData);

		return featureSet;
	}

}
