package ru.rtc.warehouse.robot.service;

import ru.rtc.warehouse.robot.controller.dto.request.RobotCreateRequest;
import ru.rtc.warehouse.robot.controller.dto.request.RobotUpdateRequest;
import ru.rtc.warehouse.robot.service.dto.RobotDTO;

import java.util.List;

public interface RobotService {
	void save(RobotCreateRequest request);
	void update(RobotUpdateRequest request, Long id);
	List<RobotDTO> findAll();
	RobotDTO findById(Long id);
	RobotDTO findByCode(String code);
	void delete(Long id);
}
