package com.neurolive.neuro_live_backend.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// Devuelve la informacion principal del perfil de usuario.
public class UserProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private Boolean active;
}
