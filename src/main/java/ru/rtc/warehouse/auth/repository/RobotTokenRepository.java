package ru.rtc.warehouse.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rtc.warehouse.auth.model.RobotToken;

import java.util.Optional;

public interface RobotTokenRepository extends JpaRepository<RobotToken, Long> {
	Optional<RobotToken> findByTokenAndRevokedFalse(String token);
	Optional<RobotToken> findByRobotId(Long robotId);
}
