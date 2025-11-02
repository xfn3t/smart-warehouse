package ru.rtc.warehouse.robot.controller.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RobotDataResponse {
    private String status; // "received"
    private List<UUID> messageId; // ID InventoryHistory
}

