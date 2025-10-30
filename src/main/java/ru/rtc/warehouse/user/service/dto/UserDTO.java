package ru.rtc.warehouse.user.service.dto;

import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class UserDTO {
	private Long id;
	private String email;
	private String name;
	private RoleDTO role;
	private Set<WarehouseDTO> warehouses;
	private boolean isDeleted;
	private LocalDateTime createdAt;
}