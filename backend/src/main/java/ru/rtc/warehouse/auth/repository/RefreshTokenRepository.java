package ru.rtc.warehouse.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.auth.model.RefreshToken;
import ru.rtc.warehouse.user.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByToken(String token);
	List<RefreshToken> findAllByUser(User user);
	void deleteAllByUser(User user);
}