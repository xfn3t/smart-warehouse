package ru.rtc.warehouse.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.ai.service.dto.RedisPredictionDTO;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionRedisService {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	private static final long REDIS_TTL = 24 * 60 * 60;
	private static final String PREDICTION_KEY_PREFIX = "ai:predictions:warehouse:";
	private static final String CRITICALITY_KEY_PREFIX = "ai:predictions:criticality:";

	public void savePredictions(String warehouseCode, Map<String, RedisPredictionDTO> predictions) {
		saveGeneralPredictions(warehouseCode, predictions);
		savePredictionsByCriticality(warehouseCode, predictions);
	}

	private void saveGeneralPredictions(String warehouseCode, Map<String, RedisPredictionDTO> predictions) {
		String redisKey = PREDICTION_KEY_PREFIX + warehouseCode;
		try {
			String predictionsJson = objectMapper.writeValueAsString(predictions);
			redisTemplate.opsForValue().set(redisKey, predictionsJson, REDIS_TTL, TimeUnit.SECONDS);
			log.info("Saved general predictions to Redis for warehouse: {} with TTL {} seconds", warehouseCode, REDIS_TTL);
		} catch (Exception e) {
			log.error("Failed to save general predictions to Redis for warehouse: {}", warehouseCode, e);
			throw new RuntimeException("Failed to save predictions to Redis", e);
		}
	}

	private void savePredictionsByCriticality(String warehouseCode, Map<String, RedisPredictionDTO> predictions) {
		Map<String, List<RedisPredictionDTO>> predictionsByCriticality = new HashMap<>();

		for (RedisPredictionDTO prediction : predictions.values()) {
			String criticality = prediction.getCriticalLevel();
			if (criticality != null) {
				predictionsByCriticality
						.computeIfAbsent(criticality, k -> new ArrayList<>())
						.add(prediction);
			}
		}

		for (Map.Entry<String, List<RedisPredictionDTO>> entry : predictionsByCriticality.entrySet()) {
			String criticality = entry.getKey();
			List<RedisPredictionDTO> criticalityPredictions = entry.getValue();

			String redisKey = CRITICALITY_KEY_PREFIX + warehouseCode + ":" + criticality.toLowerCase();
			try {
				String predictionsJson = objectMapper.writeValueAsString(criticalityPredictions);
				redisTemplate.opsForValue().set(redisKey, predictionsJson, REDIS_TTL, TimeUnit.SECONDS);
				log.info("Saved {} predictions for criticality: {} in warehouse: {}",
						criticalityPredictions.size(), criticality, warehouseCode);
			} catch (Exception e) {
				log.error("Failed to save predictions for criticality: {} in warehouse: {}", criticality, warehouseCode, e);
			}
		}
	}

	public Map<String, RedisPredictionDTO> getPredictions(String warehouseCode) {
		String redisKey = PREDICTION_KEY_PREFIX + warehouseCode;
		try {
			String predictionsJson = redisTemplate.opsForValue().get(redisKey);
			if (predictionsJson != null) {
				return objectMapper.readValue(predictionsJson, new TypeReference<Map<String, RedisPredictionDTO>>() {});
			}
			return Collections.emptyMap();
		} catch (Exception e) {
			log.error("Failed to get predictions from Redis for warehouse: {}", warehouseCode, e);
			return Collections.emptyMap();
		}
	}

	public List<RedisPredictionDTO> getPredictionsByCriticality(String warehouseCode, String criticality) {
		String redisKey = CRITICALITY_KEY_PREFIX + warehouseCode + ":" + criticality.toLowerCase();
		try {
			String predictionsJson = redisTemplate.opsForValue().get(redisKey);
			if (predictionsJson != null) {
				return objectMapper.readValue(predictionsJson, new TypeReference<List<RedisPredictionDTO>>() {});
			}
			return Collections.emptyList();
		} catch (Exception e) {
			log.error("Failed to get predictions for criticality: {} from warehouse: {}", criticality, warehouseCode, e);
			return Collections.emptyList();
		}
	}

	public Map<String, List<RedisPredictionDTO>> getAllPredictionsGroupedByCriticality(String warehouseCode) {
		Map<String, List<RedisPredictionDTO>> result = new HashMap<>();

		for (String criticality : Arrays.asList("critical", "medium", "ok")) {
			List<RedisPredictionDTO> predictions = getPredictionsByCriticality(warehouseCode, criticality);
			result.put(criticality.toUpperCase(), predictions);
		}

		return result;
	}

	public boolean hasPredictions(String warehouseCode) {
		String redisKey = PREDICTION_KEY_PREFIX + warehouseCode;
		return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
	}

	public Set<String> getAllWarehouseCodesWithPredictions() {
		try {
			Set<String> keys = redisTemplate.keys(PREDICTION_KEY_PREFIX + "*");
			return keys.stream()
					.map(key -> key.substring(PREDICTION_KEY_PREFIX.length()))
					.collect(Collectors.toSet());
		} catch (Exception e) {
			log.error("Failed to get warehouse codes from Redis", e);
		}
		return Set.of();
	}
}