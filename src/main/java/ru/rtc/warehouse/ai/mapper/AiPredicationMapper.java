package ru.rtc.warehouse.ai.mapper;

import org.mapstruct.Mapper;
import ru.rtc.warehouse.ai.model.AiPrediction;
import ru.rtc.warehouse.ai.service.dto.AiPredictionDTO;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AiPredicationMapper {

	AiPredictionDTO toDto(AiPrediction aiPrediction);
	AiPrediction toEntity(AiPredictionDTO aiPredicationDTO);

	List<AiPrediction> toEntityList(List<AiPredictionDTO> dtos);
	List<AiPredictionDTO> toDtoList(List<AiPrediction> robots);
}
