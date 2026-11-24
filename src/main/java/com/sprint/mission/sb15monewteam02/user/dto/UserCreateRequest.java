package com.sprint.mission.sb15monewteam02.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserCreateRequest(@NotBlank @Email String email,@NotBlank  String nickname, @NotBlank String password) {

}
