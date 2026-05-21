package ru.rtc.warehouse.user.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserCreateRequest {

    private String email;
    private String password;

    @JsonAlias({ "firstName", "fullName" })
    private String name;

    private String role;
}
