package com.neurolive.neuro_live_backend.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// Resume el resultado de un registro exitoso.
public class RegisterResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private String message;
}
