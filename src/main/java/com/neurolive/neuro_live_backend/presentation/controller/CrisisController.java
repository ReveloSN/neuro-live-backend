package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.business.service.CrisisService;
import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.presentation.dto.ClinicalAnalysisDTO;
import com.neurolive.neuro_live_backend.presentation.dto.CrisisCloseRequestDTO;
import com.neurolive.neuro_live_backend.presentation.dto.CrisisEventDTO;
import com.neurolive.neuro_live_backend.presentation.dto.SAMResponseDTO;
import com.neurolive.neuro_live_backend.presentation.dto.SAMResponseRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/crises")
public class CrisisController {

    private final CrisisService crisisService;

    public CrisisController(CrisisService crisisService) {
        this.crisisService = crisisService;
    }

    @GetMapping("/{crisisId}")
    public ResponseEntity<CrisisEventDTO> getCrisis(Authentication authentication,
                                                    @PathVariable Long crisisId,
                                                    HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(
                CrisisEventDTO.from(crisisService.getCrisis(authentication.getName(), crisisId, resolveIp(httpServletRequest)))
        );
    }

    @GetMapping("/patients/{patientId}")
    public ResponseEntity<List<CrisisEventDTO>> getCrisesByPatient(Authentication authentication,
                                                                   @PathVariable Long patientId,
                                                                   @RequestParam(required = false) LocalDateTime start,
                                                                   @RequestParam(required = false) LocalDateTime end,
                                                                   HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(
                crisisService.getCrisesByPatient(authentication.getName(), patientId, start, end, resolveIp(httpServletRequest))
                        .stream()
                        .map(CrisisEventDTO::from)
                        .toList()
        );
    }

    @PostMapping("/{crisisId}/close")
    public ResponseEntity<CrisisEventDTO> closeCrisis(Authentication authentication,
                                                      @PathVariable Long crisisId,
                                                      @Valid @RequestBody CrisisCloseRequestDTO request,
                                                      HttpServletRequest httpServletRequest) {
        StateEnum finalState = StateEnum.valueOf(request.finalState().trim().toUpperCase());
        return ResponseEntity.ok(
                CrisisEventDTO.from(
                        crisisService.closeCrisis(
                                authentication.getName(),
                                crisisId,
                                request.endedAt(),
                                finalState,
                                resolveIp(httpServletRequest)
                        )
                )
        );
    }

    @PostMapping("/{crisisId}/sam")
    public ResponseEntity<SAMResponseDTO> registerSam(Authentication authentication,
                                                      @PathVariable Long crisisId,
                                                      @Valid @RequestBody SAMResponseRequestDTO request,
                                                      HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(
                SAMResponseDTO.from(
                        crisisService.recordSamResponse(
                                authentication.getName(),
                                crisisId,
                                request.valence(),
                                request.arousal(),
                                resolveIp(httpServletRequest)
                        )
                )
        );
    }

    @GetMapping("/{crisisId}/sam")
    public ResponseEntity<SAMResponseDTO> getSam(Authentication authentication,
                                                 @PathVariable Long crisisId,
                                                 HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(
                SAMResponseDTO.from(
                        crisisService.getSamResponse(authentication.getName(), crisisId, resolveIp(httpServletRequest))
                )
        );
    }

    @GetMapping("/patients/{patientId}/analysis")
    public ResponseEntity<ClinicalAnalysisDTO> getAnalysis(Authentication authentication,
                                                           @PathVariable Long patientId,
                                                           @RequestParam(required = false) LocalDateTime start,
                                                           @RequestParam(required = false) LocalDateTime end,
                                                           HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(
                ClinicalAnalysisDTO.from(
                        crisisService.buildAnalysis(
                                authentication.getName(),
                                patientId,
                                start,
                                end,
                                resolveIp(httpServletRequest)
                        )
                )
        );
    }

    @GetMapping(value = "/patients/{patientId}/export", produces = "text/csv")
    public ResponseEntity<String> exportCsv(Authentication authentication,
                                            @PathVariable Long patientId,
                                            @RequestParam(required = false) LocalDateTime start,
                                            @RequestParam(required = false) LocalDateTime end,
                                            HttpServletRequest httpServletRequest) {
        String csv = crisisService.exportCsv(authentication.getName(), patientId, start, end, resolveIp(httpServletRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"crisis-events-" + patientId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    private String resolveIp(HttpServletRequest httpServletRequest) {
        return httpServletRequest == null ? "unknown" : httpServletRequest.getRemoteAddr();
    }
}
