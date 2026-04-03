package com.neurolive.neuro_live_backend.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Representa los datos editables del perfil del usuario.
public class UpdateUserProfileRequest {

    @NotBlank
    private String name;

    private String photoUrl;
}
