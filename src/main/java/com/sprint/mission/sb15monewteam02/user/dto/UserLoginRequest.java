package com.sprint.mission.sb15monewteam02.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequest(@NotBlank @Email String email,
                               @NotBlank String password) {
}
