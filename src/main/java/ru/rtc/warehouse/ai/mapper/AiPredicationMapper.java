package ru.rtc.warehouse.ai.mapper;

import org.mapstruct.Mapper;
import ru.rtc.warehouse.ai.model.AiPrediction;
import ru.rtc.warehouse.ai.service.dto.AiPredicationDTO;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AiPredicationMapper {

	AiPredicationDTO toDto(AiPrediction aiPrediction);
	AiPrediction toEntity(AiPredicationDTO aiPredicationDTO);

	List<AiPrediction> toEntityList(List<AiPredicationDTO> dtos);
	List<AiPredicationDTO> toDtoList(List<AiPrediction> robots);
}
