package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.business.service.ClinicalAccessService;
import com.neurolive.neuro_live_backend.business.service.UserService;
import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.presentation.dto.UpdateUserProfileRequest;
import com.neurolive.neuro_live_backend.presentation.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/users")
// Maneja los endpoints relacionados con el perfil del usuario autenticado.
public class UserController {

    private final UserService userService;
    private final ClinicalAccessService clinicalAccessService;

    public UserController(UserService userService, ClinicalAccessService clinicalAccessService) {
        this.userService = userService;
        this.clinicalAccessService = clinicalAccessService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUserProfile(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(Authentication authentication,
                                                                 @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUserProfile(authentication.getName(), request));
    }

    // Devuelve la especialidad del doctor autenticado.
    @GetMapping("/me/doctor-profile")
    public ResponseEntity<Map<String, String>> getDoctorProfile(Authentication authentication) {
        String specialty = userService.getDoctorSpecialty(authentication.getName());
        return ResponseEntity.ok(Map.of("specialty", specialty != null ? specialty : ""));
    }

    // Actualiza la especialidad del doctor autenticado.
    @PutMapping("/me/doctor-profile")
    public ResponseEntity<Map<String, String>> updateDoctorProfile(Authentication authentication,
            @RequestBody Map<String, String> body) {
        String specialty = body.getOrDefault("specialty", "").trim();
        String updated = userService.updateDoctorSpecialty(authentication.getName(), specialty);
        return ResponseEntity.ok(Map.of("specialty", updated != null ? updated : ""));
    }

    // Devuelve el perfil de consentimiento clinico del paciente. Solo accesible con acceso clinico al paciente.
    @GetMapping("/patients/{patientId}/clinical-profile")
    public ResponseEntity<UserService.PatientConsentResponse> getPatientClinicalProfile(Authentication authentication,
            @PathVariable Long patientId) {
        clinicalAccessService.requirePatientAccess(authentication.getName(), patientId);
        return ResponseEntity.ok(userService.getPatientConsent(authentication.getName(), patientId));
    }

    // El propio paciente puede actualizar su consentimiento de monitoreo biometrico.
    @PutMapping("/patients/{patientId}/consent")
    public ResponseEntity<UserService.PatientConsentResponse> updatePatientConsent(Authentication authentication,
            @PathVariable Long patientId) {
        User requester = clinicalAccessService.resolveCurrentUser(authentication.getName());
        if (requester.getRole() != RoleEnum.PATIENT || !requester.getId().equals(patientId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the patient can update their own consent");
        }
        return ResponseEntity.ok(userService.grantPatientConsent(authentication.getName(), patientId));
    }
}
