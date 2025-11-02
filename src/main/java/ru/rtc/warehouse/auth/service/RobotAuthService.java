package ru.rtc.warehouse.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.auth.model.RobotToken;
import ru.rtc.warehouse.auth.repository.RobotTokenRepository;
import ru.rtc.warehouse.auth.util.JwtUtil;
import ru.rtc.warehouse.robot.model.Robot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RobotAuthService {

	private final JwtUtil jwtUtil;
	private final RobotTokenRepository robotTokenRepository;

	public RobotToken createRobotToken(Robot robot) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("roles", List.of("ROBOT"));
		claims.put("warehouse_id", robot.getWarehouse().getId());
		claims.put("robot_code", robot.getCode());

		String token = jwtUtil.generatePermanentToken(robot.getCode(), claims);

		RobotToken robotToken = RobotToken.builder()
				.robot(robot)
				.token(token)
				.build();

		return robotTokenRepository.save(robotToken);
	}
}
