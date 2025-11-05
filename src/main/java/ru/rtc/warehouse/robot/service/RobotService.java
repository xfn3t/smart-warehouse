package ru.rtc.warehouse.robot.service;

import ru.rtc.warehouse.robot.controller.dto.request.RobotCreateRequest;
import ru.rtc.warehouse.robot.controller.dto.request.RobotUpdateRequest;
import ru.rtc.warehouse.robot.controller.dto.response.RobotTokenResponse;
import ru.rtc.warehouse.robot.service.dto.RobotDTO;

import java.util.List;

public interface RobotService {
	RobotTokenResponse save(RobotCreateRequest request);
	RobotDTO update(RobotUpdateRequest request, Long id);
	RobotDTO update(RobotUpdateRequest request, String robotCode);
	List<RobotDTO> findAll();
	RobotDTO findById(Long id);
	RobotDTO findByCode(String code);
	void delete(Long id);
	void delete(String robotCode);

	List<RobotDTO> findAllByWarehouseCode(String warehouseCode);
}
