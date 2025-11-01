package ru.rtc.warehouse.robot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RobotUpdateDataDTO {
    private String robot_id;
    private Integer battery_level;
    private Integer zone;
    private Integer row;
    private Integer shelf;
    private Object next_checkpoint; 
    private String timestamp;
    private List<Object> recent_scans; 
}