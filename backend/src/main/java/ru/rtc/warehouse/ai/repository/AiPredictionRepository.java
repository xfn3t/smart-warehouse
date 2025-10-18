package ru.rtc.warehouse.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.ai.model.AiPrediction;

@Repository
public interface AiPredictionRepository extends JpaRepository<AiPrediction, Long> {
}
