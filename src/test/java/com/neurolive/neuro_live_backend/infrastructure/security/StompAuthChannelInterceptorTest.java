package com.neurolive.neuro_live_backend.infrastructure.security;

import com.neurolive.neuro_live_backend.business.service.ClinicalAccessService;
import com.neurolive.neuro_live_backend.data.exception.UnauthorizedAccessException;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Verifica que los topics clinicos STOMP no filtren datos de otros pacientes.
class StompAuthChannelInterceptorTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final ClinicalAccessService clinicalAccessService = mock(ClinicalAccessService.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private final StompAuthChannelInterceptor interceptor =
            new StompAuthChannelInterceptor(jwtService, userDetailsService, clinicalAccessService);

    @Test
    void shouldDelegatePatientTopicAuthorizationToClinicalAccessRules() {
        Message<byte[]> message = subscribeMessage(
                "/topic/patients/99/caregiver",
                "doctor@neurolive.test",
                "ROLE_DOCTOR"
        );

        interceptor.preSend(message, channel);

        verify(clinicalAccessService).requirePatientAccess("doctor@neurolive.test", 99L);
    }

    @Test
    void shouldRejectSubscriptionWhenClinicalAccessIsDenied() {
        Message<byte[]> message = subscribeMessage(
                "/topic/patients/99/caregiver",
                "patient@neurolive.test",
                "ROLE_PATIENT"
        );
        when(clinicalAccessService.requirePatientAccess("patient@neurolive.test", 99L))
                .thenThrow(new UnauthorizedAccessException("User can only access their own clinical data"));

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, channel));
    }

    private Message<byte[]> subscribeMessage(String destination, String email, String role) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority(role))
        ));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
