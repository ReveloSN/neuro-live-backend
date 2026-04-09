package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.business.service.UserLinkService;
import com.neurolive.neuro_live_backend.presentation.dto.LinkRedeemRequestDTO;
import com.neurolive.neuro_live_backend.presentation.dto.LinkTokenResponseDTO;
import com.neurolive.neuro_live_backend.presentation.dto.UserLinkResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/links")
// Expone el flujo de vinculacion por token entre paciente y cuidador o medico.
public class UserLinkController {

    private final UserLinkService userLinkService;

    public UserLinkController(UserLinkService userLinkService) {
        this.userLinkService = userLinkService;
    }

    @PostMapping("/tokens")
    public ResponseEntity<LinkTokenResponseDTO> issueToken(Authentication authentication,
                                                           HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                LinkTokenResponseDTO.from(
                        userLinkService.issueToken(authentication.getName(), resolveIp(httpServletRequest))
                )
        );
    }

    @PostMapping("/redeem")
    public ResponseEntity<UserLinkResponseDTO> redeemToken(Authentication authentication,
                                                           @Valid @RequestBody LinkRedeemRequestDTO request,
                                                           HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(
                UserLinkResponseDTO.from(
                        userLinkService.redeemToken(
                                authentication.getName(),
                                request.token(),
                                resolveIp(httpServletRequest)
                        )
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<List<UserLinkResponseDTO>> getMyLinks(Authentication authentication) {
        return ResponseEntity.ok(
                userLinkService.getLinksForCurrentUser(authentication.getName())
                        .stream()
                        .map(UserLinkResponseDTO::from)
                        .toList()
        );
    }

    private String resolveIp(HttpServletRequest httpServletRequest) {
        return httpServletRequest == null ? "unknown" : httpServletRequest.getRemoteAddr();
    }
}
