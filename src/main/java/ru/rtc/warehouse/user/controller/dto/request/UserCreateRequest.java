package ru.rtc.warehouse.user.controller.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserCreateRequest {
	private String email;
	private String password;
	private String name;
	private String role;
}
