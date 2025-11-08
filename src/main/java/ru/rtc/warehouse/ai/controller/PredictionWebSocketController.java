package ru.rtc.warehouse.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import ru.rtc.warehouse.ai.controller.dto.websocket.WebSocketMessageDTO;
import ru.rtc.warehouse.ai.service.PredictionRedisService;
import ru.rtc.warehouse.ai.service.dto.RedisPredictionDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PredictionWebSocketController {

	private final SimpMessagingTemplate messagingTemplate;
	private final PredictionRedisService predictionRedisService;

	private static final String WS_PREDICTIONS_TOPIC = "/topic/dashboard/predictions";
	private static final String WS_CRITICALITY_TOPIC = "/topic/dashboard/predictions/criticality";

	@Scheduled(fixedRate = 60000)
	public void sendPeriodicUpdates() {
		try {
			Set<String> warehouseCodes = predictionRedisService.getAllWarehouseCodesWithPredictions();
			log.info("Sending periodic updates for {} warehouses", warehouseCodes.size());

			for (String warehouseCode : warehouseCodes) {
				Map<String, RedisPredictionDTO> predictions = predictionRedisService.getPredictions(warehouseCode);
				if (predictions != null && !predictions.isEmpty()) {
					sendPredictionsUpdate(warehouseCode, predictions);
				}

				sendCriticalityUpdates(warehouseCode);
			}
		} catch (Exception e) {
			log.error("Failed to send periodic prediction updates", e);
		}
	}

	@MessageMapping("/predictions/subscribe/{warehouseCode}")
	@SendTo("/topic/dashboard/predictions/{warehouseCode}")
	public WebSocketMessageDTO handleSubscription(@DestinationVariable String warehouseCode) {
		try {
			Map<String, RedisPredictionDTO> predictions = predictionRedisService.getPredictions(warehouseCode);
			if (predictions != null && !predictions.isEmpty()) {
				return WebSocketMessageDTO.builder()
						.type("prediction_data")
						.warehouseCode(warehouseCode)
						.data(predictions)
						.timestamp(System.currentTimeMillis())
						.status("success")
						.build();
			} else {
				return WebSocketMessageDTO.builder()
						.type("prediction_data")
						.warehouseCode(warehouseCode)
						.status("no_data")
						.message("No prediction data available")
						.timestamp(System.currentTimeMillis())
						.build();
			}
		} catch (Exception e) {
			log.error("Failed to handle subscription for warehouse: {}", warehouseCode, e);
			return WebSocketMessageDTO.builder()
					.type("prediction_data")
					.warehouseCode(warehouseCode)
					.status("error")
					.message("Failed to load prediction data")
					.timestamp(System.currentTimeMillis())
					.build();
		}
	}

	@MessageMapping("/predictions/criticality/{warehouseCode}/{criticality}")
	@SendTo("/topic/dashboard/predictions/criticality/{warehouseCode}/{criticality}")
	public WebSocketMessageDTO handleCriticalitySubscription(
			@DestinationVariable String warehouseCode,
			@DestinationVariable String criticality) {

		try {
			List<RedisPredictionDTO> predictions = predictionRedisService.getPredictionsByCriticality(warehouseCode, criticality);
			return WebSocketMessageDTO.builder()
					.type("criticality_data")
					.warehouseCode(warehouseCode)
					.criticality(criticality.toUpperCase())
					.data(predictions)
					.timestamp(System.currentTimeMillis())
					.status("success")
					.build();
		} catch (Exception e) {
			log.error("Failed to handle criticality subscription for warehouse: {} and criticality: {}",
					warehouseCode, criticality, e);
			return WebSocketMessageDTO.builder()
					.type("criticality_data")
					.warehouseCode(warehouseCode)
					.criticality(criticality.toUpperCase())
					.status("error")
					.message("Failed to load criticality data")
					.timestamp(System.currentTimeMillis())
					.build();
		}
	}

	@MessageMapping("/predictions/criticality/{warehouseCode}")
	@SendTo("/topic/dashboard/predictions/criticality/{warehouseCode}")
	public WebSocketMessageDTO handleAllCriticalitySubscription(@DestinationVariable String warehouseCode) {
		try {
			Map<String, List<RedisPredictionDTO>> allCriticalityData =
					predictionRedisService.getAllPredictionsGroupedByCriticality(warehouseCode);

			return WebSocketMessageDTO.builder()
					.type("all_criticality_data")
					.warehouseCode(warehouseCode)
					.data(allCriticalityData)
					.timestamp(System.currentTimeMillis())
					.status("success")
					.build();
		} catch (Exception e) {
			log.error("Failed to handle all criticality subscription for warehouse: {}", warehouseCode, e);
			return WebSocketMessageDTO.builder()
					.type("all_criticality_data")
					.warehouseCode(warehouseCode)
					.status("error")
					.message("Failed to load all criticality data")
					.timestamp(System.currentTimeMillis())
					.build();
		}
	}

	@MessageMapping("/predictions/refresh/{warehouseCode}")
	@SendTo("/topic/dashboard/predictions/{warehouseCode}")
	public WebSocketMessageDTO handleRefresh(@DestinationVariable String warehouseCode) {
		try {
			Map<String, RedisPredictionDTO> predictions = predictionRedisService.getPredictions(warehouseCode);
			if (predictions != null && !predictions.isEmpty()) {
				return WebSocketMessageDTO.builder()
						.type("prediction_refresh")
						.warehouseCode(warehouseCode)
						.data(predictions)
						.timestamp(System.currentTimeMillis())
						.status("refreshed")
						.build();
			} else {
				return WebSocketMessageDTO.builder()
						.type("prediction_refresh")
						.warehouseCode(warehouseCode)
						.status("no_data")
						.message("No prediction data available")
						.timestamp(System.currentTimeMillis())
						.build();
			}
		} catch (Exception e) {
			log.error("Failed to handle refresh for warehouse: {}", warehouseCode, e);
			return WebSocketMessageDTO.builder()
					.type("prediction_refresh")
					.warehouseCode(warehouseCode)
					.status("error")
					.message("Failed to refresh prediction data")
					.timestamp(System.currentTimeMillis())
					.build();
		}
	}

	private void sendPredictionsUpdate(String warehouseCode, Map<String, RedisPredictionDTO> predictions) {
		try {
			WebSocketMessageDTO wsMessage = WebSocketMessageDTO.builder()
					.type("prediction_update")
					.warehouseCode(warehouseCode)
					.data(predictions)
					.timestamp(System.currentTimeMillis())
					.build();

			messagingTemplate.convertAndSend(WS_PREDICTIONS_TOPIC, wsMessage);
			messagingTemplate.convertAndSend(WS_PREDICTIONS_TOPIC + "/" + warehouseCode, wsMessage);

			log.debug("Sent WebSocket update for warehouse: {}", warehouseCode);
		} catch (Exception e) {
			log.error("Failed to send WebSocket update for warehouse: {}", warehouseCode, e);
		}
	}

	private void sendCriticalityUpdates(String warehouseCode) {
		try {
			Map<String, List<RedisPredictionDTO>> allCriticalityData =
					predictionRedisService.getAllPredictionsGroupedByCriticality(warehouseCode);

			WebSocketMessageDTO wsMessage = WebSocketMessageDTO.builder()
					.type("criticality_update")
					.warehouseCode(warehouseCode)
					.data(allCriticalityData)
					.timestamp(System.currentTimeMillis())
					.build();

			messagingTemplate.convertAndSend(WS_CRITICALITY_TOPIC, wsMessage);
			messagingTemplate.convertAndSend(WS_CRITICALITY_TOPIC + "/" + warehouseCode, wsMessage);

			for (Map.Entry<String, List<RedisPredictionDTO>> entry : allCriticalityData.entrySet()) {
				String criticality = entry.getKey();
				List<RedisPredictionDTO> predictions = entry.getValue();

				WebSocketMessageDTO criticalityMessage = WebSocketMessageDTO.builder()
						.type("criticality_level_update")
						.warehouseCode(warehouseCode)
						.criticality(criticality)
						.data(predictions)
						.timestamp(System.currentTimeMillis())
						.build();

				messagingTemplate.convertAndSend(
						WS_CRITICALITY_TOPIC + "/" + warehouseCode + "/" + criticality.toLowerCase(),
						criticalityMessage
				);
			}

			log.debug("Sent criticality updates for warehouse: {}", warehouseCode);
		} catch (Exception e) {
			log.error("Failed to send criticality updates for warehouse: {}", warehouseCode, e);
		}
	}
}