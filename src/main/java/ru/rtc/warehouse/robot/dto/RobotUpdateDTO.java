package ru.rtc.warehouse.robot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RobotUpdateDTO {
    private String type; 
    private RobotUpdateDataDTO data;
}
