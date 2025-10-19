package ru.rtc.warehouse.user.mapper;

import org.mapstruct.Mapper;
import ru.rtc.warehouse.user.controller.dto.request.UserCreateRequest;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.user.service.dto.UserDTO;

@Mapper(componentModel = "spring")
public interface UserMapper {
	UserDTO toDto(User user);
	User toEntity(UserDTO userDTO);
	User toEntity(UserCreateRequest request);
}
