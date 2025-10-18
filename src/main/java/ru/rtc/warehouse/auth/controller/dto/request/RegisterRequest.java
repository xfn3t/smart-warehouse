package ru.rtc.warehouse.auth.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.user.model.User;

@Getter
@Setter
@AllArgsConstructor
public class RegisterRequest {
	private String email;
	private String password;
	private String name;
	private User.Role role;
}