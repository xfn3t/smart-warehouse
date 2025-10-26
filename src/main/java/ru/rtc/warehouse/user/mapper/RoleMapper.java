package ru.rtc.warehouse.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.rtc.warehouse.user.model.Role;
import ru.rtc.warehouse.user.service.dto.RoleDTO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleMapper {
	RoleDTO toDto(Role entity);
	Role toEntity(RoleDTO dto);
}