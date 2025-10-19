package ru.rtc.warehouse.auth.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.user.model.User;

@Getter
@Setter
@AllArgsConstructor
public class RegisterRequest {

	@Email(message = "Email should be valid")
	@Size(min = 11, message = "Email must be at least 11 characters long")
	private String email;

	@Size(min = 5, max = 20, message = "Password can be from 5 to 20 size interval")
	@NotNull(message = "Password can't be null")
	private String password;

	@NotNull(message = "Name can't be null")
	private String name;

	private User.Role role;
}