package ru.rtc.warehouse.robot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.rtc.warehouse.config.RobotProperties;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.inventory.common.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.InventoryHistoryEntityService;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.repository.ProductRepository;
import ru.rtc.warehouse.robot.controller.dto.LocationDTO;
import ru.rtc.warehouse.robot.controller.dto.ScanResultDTO;
import ru.rtc.warehouse.robot.controller.dto.request.RobotDataRequest;
import ru.rtc.warehouse.robot.controller.dto.response.RobotDataResponse;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.repository.RobotRepository;
import ru.rtc.warehouse.robot.service.RobotDataService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
* Service for receiving data from the robot.
* - Updates the Robot record
* - Saves InventoryHistory for each scanResult
* - Calculates the diff relative to the last record (product + zone)
* - Pushes the last 5 scans to a Redis list
* - Publishes an event to the WebSocket (SimpMessagingTemplate)
*
*/
@Service
@RequiredArgsConstructor
@Slf4j
public class RobotDataServiceImpl implements RobotDataService {

    private final RobotProperties robotProperties;

    private final RobotRepository robotRepository;
    private final InventoryHistoryEntityService inventoryHistoryEntityService;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    private final ProductRepository productRepository;

    
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    @Transactional
    public RobotDataResponse processRobotData(RobotDataRequest request) {
        
        Robot robot = robotRepository.findByCode(request.getCode())
                .orElseThrow(() -> new NotFoundException("Robot not found: " + request.getCode()));

        
        robot.setBatteryLevel(request.getBatteryLevel());
        LocationDTO loc = request.getLocation();
        robot.setCurrentZone(loc.getZone());
        robot.setCurrentRow(loc.getRow());
        robot.setCurrentShelf(loc.getShelf());
        robot.setLastUpdate(LocalDateTime.ofInstant(request.getTimestamp(), ZoneOffset.UTC));

        robotRepository.save(robot);

        
        UUID messageId = UUID.randomUUID();

        
        List<Map<String, Object>> recentScansPayload = new ArrayList<>(request.getScanResults().size());
        LocalDateTime scannedAt = LocalDateTime.ofInstant(request.getTimestamp(), ZoneOffset.UTC);

        for (ScanResultDTO sr : request.getScanResults()) {
            String productCode = sr.getProductCode(); 
            String productName = sr.getProductName();
            Integer quantity = sr.getQuantity();
            InventoryHistoryStatus status = sr.getStatus();


            Optional<InventoryHistory> lastOpt = inventoryHistoryRepository.findFirstByProduct_CodeAndZoneOrderByScannedAtDesc(productCode, request.getLocation().getZone()); 

            Integer previousQty = lastOpt.map(InventoryHistory::getQuantity).orElse(null);
            Integer diff = (previousQty == null) ? null : (quantity - previousQty);

            
            InventoryHistory history = new InventoryHistory();
          
            Product product = productRepository.findByCode(productCode)
                        .orElseThrow(() -> new NotFoundException("Product not found by sku: " + productCode));


            history.setRobot(robot);
            history.setProduct(product);
            history.setQuantity(quantity);
            history.setZone(loc.getZone());
            history.setRowNumber(loc.getRow());
            history.setShelfNumber(loc.getShelf());
            history.setStatus(status);
            history.setScannedAt(scannedAt);
            history.setMessageId(messageId);

            inventoryHistoryEntityService.save(history);

            
            Map<String, Object> scanMap = new HashMap<>();
            scanMap.put("productCode", productCode);
            scanMap.put("productName", productName);
            scanMap.put("quantity", quantity);
            scanMap.put("status", status);
            scanMap.put("diff", diff);
            scanMap.put("scannedAt", scannedAt.toString());
            recentScansPayload.add(scanMap);
        }

        
        String redisKey = String.format(robotProperties.getRecentScansKeyTemplate(), robot.getCode());
        try {
           
            List<String> jsonList = recentScansPayload.stream()
                    .map(scanMap -> {
                        try { return objectMapper.writeValueAsString(scanMap); }
                        catch (JsonProcessingException e) { log.warn("serialize error: {}", e.getMessage()); return null; }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!jsonList.isEmpty()) {
              
                redisTemplate.opsForList().rightPushAll(redisKey, jsonList);
              
                long keep = robotProperties.getRecentScansLimit();
              
                redisTemplate.opsForList().trim(redisKey, -keep, -1);
              
                redisTemplate.expire(redisKey, java.time.Duration.ofDays(robotProperties.getRecentScansTtlDays()));
            }
        } catch (Exception e) {
            log.warn("Redis push failed for robot {}: {}", robot.getCode(), e.getMessage());
        }


       
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "robot_update");

        Map<String, Object> data = new HashMap<>();
        data.put("robot_id", robot.getCode());
        data.put("battery_level", robot.getBatteryLevel());
        data.put("zone", robot.getCurrentZone());
        data.put("row", robot.getCurrentRow());
        data.put("shelf", robot.getCurrentShelf());
        data.put("next_checkpoint", request.getNextCheckpoint());
        data.put("timestamp", scannedAt.toString());
        
        data.put("recent_scans", recentScansPayload.stream().limit(robotProperties.getRecentScansLimit()).collect(Collectors.toList()));

        wsPayload.put("data", data);

        try {
            String json = objectMapper.writeValueAsString(wsPayload);
           
            redisTemplate.convertAndSend(robotProperties.getRedisChannel(), json);
        } catch (Exception e) {
            log.warn("Publish to redis channel failed, trying local WS publish for robot {}: {}", robot.getCode(), e.getMessage());
           
            try {
                messagingTemplate.convertAndSend(robotProperties.getWsGlobalTopic(), wsPayload);
                String perRobotTopic = robotProperties.getWsRobotTopicPrefix() + "/" + robot.getCode();
                messagingTemplate.convertAndSend(perRobotTopic, wsPayload);
            } catch (Exception ex) {
                log.warn("Fallback WS publish also failed: {}", ex.getMessage());
            }
        }


        return new RobotDataResponse("received", messageId);
    }

   
}
