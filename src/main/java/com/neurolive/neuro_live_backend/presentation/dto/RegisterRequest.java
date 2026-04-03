package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Contiene la informacion necesaria para registrar un nuevo usuario.
public class RegisterRequest {

    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private RoleEnum role;
}
