package ru.rtc.warehouse.robot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.repository.RobotRepository;
import ru.rtc.warehouse.robot.service.RobotEntityService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RobotEntityServiceImpl implements RobotEntityService {

	private final RobotRepository robotRepository;


	@Override
	public void save(Robot robot) {
		robotRepository.save(robot);
	}

	@Override
	public void update(Robot robot) {
		robotRepository.save(robot);
	}

	@Override
	public List<Robot> findAll() {
		return robotRepository.findAll();
	}

	@Override
	public Robot findById(Long id) {
		return robotRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Robot not found"));
	}

	@Override
	public Robot findByCode(String code) {
		return robotRepository.findByCode(code)
				.orElseThrow(() -> new NotFoundException("Robot not found"));
	}

	@Override
	public Integer findMaxRobotNumber() {
		return robotRepository.findMaxRobotNumber();
	}

	@Override
	public void delete(Long id) {
		robotRepository.deleteById(id);
	}

}
