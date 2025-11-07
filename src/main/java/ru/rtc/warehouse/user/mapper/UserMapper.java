package ru.rtc.warehouse.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import ru.rtc.warehouse.user.controller.dto.request.UserCreateRequest;
import ru.rtc.warehouse.user.model.Role;
import ru.rtc.warehouse.user.model.Role.RoleCode;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.user.service.dto.UserDTO;
import ru.rtc.warehouse.warehouse.mapper.WarehouseMapper;

import java.util.List;

@Mapper(
		componentModel = "spring",
		uses = {RoleMapper.class, WarehouseMapper.class},
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

	UserDTO toDto(User user);

	@Mapping(target = "password", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	User toEntity(UserDTO dto);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "role", ignore = true)
	@Mapping(target = "warehouses", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	User toEntity(UserCreateRequest request);

	@Named("mapStringToRole")
	default Role mapStringToRole(String roleCode) {
		if (roleCode == null) {
			return createDefaultRole();
		}

		try {
			RoleCode code = RoleCode.valueOf(roleCode.toUpperCase());
			return Role.builder()
					.code(code)
					.build();
		} catch (IllegalArgumentException e) {
			return createDefaultRole();
		}
	}

	private Role createDefaultRole() {
		return Role.builder()
				.code(RoleCode.VIEWER)
				.build();
	}

	List<UserDTO> toDtoList(List<User> allByWarehouse);
}