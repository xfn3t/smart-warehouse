package ru.rtc.warehouse.robot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.config.RobotProperties;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.inventory.mapper.InventoryStatusReferenceMapper;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.service.InventoryHistoryEntityService;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.robot.controller.dto.ScanResultDTO;
import ru.rtc.warehouse.robot.controller.dto.request.RobotDataRequest;
import ru.rtc.warehouse.robot.controller.dto.response.RobotDataResponse;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.RobotDataService;
import ru.rtc.warehouse.robot.service.RobotEntityService;
import ru.rtc.warehouse.robot.service.adapter.InventoryHistoryAdapter;
import ru.rtc.warehouse.robot.service.adapter.ProductAdapter;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Service
@RequiredArgsConstructor
@Slf4j
public class RobotDataServiceImpl implements RobotDataService {

    private final RobotProperties robotProperties;

    private final RobotEntityService robotEntityService;
    private final InventoryHistoryEntityService inventoryHistoryEntityService;
    private final InventoryHistoryAdapter inventoryHistoryAdapter;
    private final ProductAdapter productAdapter;

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    private final InventoryStatusReferenceMapper inventoryStatusReferenceMapper;

    @Override
    @Transactional
    public RobotDataResponse processRobotData(RobotDataRequest request) {
        Robot robot = Optional.ofNullable(
            robotEntityService.findByCode(request.getCode())
        ).orElseThrow(() ->
            new NotFoundException("Robot not found: " + request.getCode())
        );

        robot.setBatteryLevel(request.getBatteryLevel());

        LocalDateTime scannedAt = LocalDateTime.ofInstant(
            request.getTimestamp(),
            ZoneOffset.UTC
        );
        robot.setLastUpdate(scannedAt);

        Warehouse robotWarehouse = robot.getWarehouse();
        if (robotWarehouse == null) {
            throw new NotFoundException(
                "Robot is not assigned to any warehouse: " + robot.getCode()
            );
        }

        Integer zone = request.getLocation().getZone();
        Integer row = request.getLocation().getRow();
        Integer shelf = request.getLocation().getShelf();
        validateLocationBounds(zone, row, shelf, robotWarehouse);

        robot.setCurrentZone(zone);
        robot.setCurrentRow(row);
        robot.setCurrentShelf(shelf);

        robotEntityService.saveAndFlush(robot);

        List<UUID> messageIds = new ArrayList<>(
            request.getScanResults().size()
        );

        List<Map<String, Object>> recentScansPayload = new ArrayList<>(
            request.getScanResults().size()
        );

        for (ScanResultDTO sr : request.getScanResults()) {
            UUID messageId = UUID.randomUUID();
            messageIds.add(messageId);
            String productCode = sr.getProductCode();
            String productName = sr.getProductName();
            Integer quantity = sr.getQuantity();
            InventoryHistoryStatus status =
                inventoryStatusReferenceMapper.mapStringToInventoryStatus(
                    sr.getStatusCode()
                );

            Optional<
                ru.rtc.warehouse.inventory.model.InventoryHistory
            > lastOpt =
                inventoryHistoryAdapter.findLatestByProductCodeAndZoneAndRowAndShelfAndWarehouse(
                    productCode,
                    zone,
                    row,
                    shelf,
                    robotWarehouse
                );
            Integer previousQty = lastOpt
                .map(
                    ru.rtc.warehouse.inventory.model.InventoryHistory::getQuantity
                )
                .orElse(null);
            Integer expectedQty = previousQty != null ? previousQty : 0;
            Integer diff = quantity == null ? null : (quantity - expectedQty);

            Product product = productAdapter
                .findByCodeAndWarehouse(productCode, robotWarehouse)
                .orElseGet(() ->
                    productAdapter
                        .findByCode(productCode)
                        .orElseThrow(() ->
                            new NotFoundException(
                                "Product not found by sku-code: " + productCode
                            )
                        )
                );

            InventoryHistory history = new InventoryHistory();
            history.setRobot(robot);
            history.setProduct(product);
            history.setWarehouse(robotWarehouse);
            history.setZone(zone);
            history.setRow(row);
            history.setShelf(shelf);
            history.setExpectedQuantity(expectedQty);
            history.setQuantity(quantity);
            history.setDifference(diff);
            history.setStatus(status);
            history.setScannedAt(scannedAt);
            history.setMessageId(messageId);

            inventoryHistoryEntityService.save(history);

            Map<String, Object> scanMap = new HashMap<>();
            scanMap.put("productCode", productCode);
            scanMap.put("productName", productName);
            scanMap.put("quantity", quantity);
            scanMap.put(
                "status",
                status != null
                    ? status.getCode() != null
                        ? status.getCode().name()
                        : null
                    : null
            );
            scanMap.put("diff", diff);
            scanMap.put("scannedAt", scannedAt.toString());
            recentScansPayload.add(scanMap);
        }

        String redisKey = String.format(
            robotProperties.getRecentScansKeyTemplate(),
            robot.getCode()
        );
        try {
            List<String> jsonList = recentScansPayload
                .stream()
                .map(scanMap -> {
                    try {
                        return objectMapper.writeValueAsString(scanMap);
                    } catch (JsonProcessingException e) {
                        log.warn("serialize error: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            if (!jsonList.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(redisKey, jsonList);
                long keep = robotProperties.getRecentScansLimit();
                redisTemplate.opsForList().trim(redisKey, -keep, -1);
                redisTemplate.expire(
                    redisKey,
                    java.time.Duration.ofDays(
                        robotProperties.getRecentScansTtlDays()
                    )
                );
            }
        } catch (Exception e) {
            log.warn(
                "Redis push failed for robot {}: {}",
                robot.getCode(),
                e.getMessage()
            );
        }

        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "robot_update");
        Map<String, Object> data = new HashMap<>();
        data.put("robot_id", robot.getCode());
        data.put(
            "warehouse_code",
            robot.getWarehouse() != null ? robot.getWarehouse().getCode() : null
        );
        data.put("battery_level", robot.getBatteryLevel());
        data.put("zone", robot.getCurrentZone());
        data.put("row", robot.getCurrentRow());
        data.put("shelf", robot.getCurrentShelf());
        data.put("next_checkpoint", request.getNextCheckpoint());
        data.put("timestamp", scannedAt.toString());
        data.put(
            "recent_scans",
            recentScansPayload
                .stream()
                .limit(robotProperties.getRecentScansLimit())
                .collect(Collectors.toList())
        );
        wsPayload.put("data", data);

        try {
            String json = objectMapper.writeValueAsString(wsPayload);
            redisTemplate.convertAndSend(
                robotProperties.getRedisChannel(),
                json
            );
        } catch (Exception e) {
            log.warn(
                "Publish to redis channel failed, trying local WS publish for robot {}: {}",
                robot.getCode(),
                e.getMessage()
            );
            try {
                messagingTemplate.convertAndSend(
                    robotProperties.getWsGlobalTopic(),
                    wsPayload
                );
                String perRobotTopic =
                    robotProperties.getWsRobotTopicPrefix() +
                    "/" +
                    robot.getCode();
                messagingTemplate.convertAndSend(perRobotTopic, wsPayload);
            } catch (Exception ex) {
                log.warn(
                    "Fallback WS publish also failed: {}",
                    ex.getMessage()
                );
            }
        }

        return new RobotDataResponse("received", messageIds);
    }

    private void validateLocationBounds(
        Integer zone,
        Integer row,
        Integer shelf,
        Warehouse warehouse
    ) {
        if (zone < 0 || zone > warehouse.getZoneMaxSize()) {
            throw new IllegalArgumentException(
                "Zone out of bounds for warehouse " +
                    warehouse.getCode() +
                    ". Allowed: 0.." +
                    warehouse.getZoneMaxSize() +
                    ", got: " +
                    zone
            );
        }
        if (row != null && (row < 0 || row > warehouse.getRowMaxSize())) {
            throw new IllegalArgumentException(
                "Row out of bounds for warehouse " +
                    warehouse.getCode() +
                    ". Allowed: 0.." +
                    warehouse.getRowMaxSize() +
                    ", got: " +
                    row
            );
        }
        if (
            shelf != null && (shelf < 0 || shelf > warehouse.getShelfMaxSize())
        ) {
            throw new IllegalArgumentException(
                "Shelf out of bounds for warehouse " +
                    warehouse.getCode() +
                    ". Allowed: 0.." +
                    warehouse.getShelfMaxSize() +
                    ", got: " +
                    shelf
            );
        }
    }
}
