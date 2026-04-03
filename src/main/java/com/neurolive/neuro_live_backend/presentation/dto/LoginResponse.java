package com.neurolive.neuro_live_backend.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// Devuelve los datos del usuario autenticado y su token de acceso.
public class LoginResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private String token;
    private String message;
}
