package com.neurolive.neuro_live_backend.infrastructure.security;

import com.neurolive.neuro_live_backend.business.service.ClinicalAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
// Valida el JWT en cada conexion STOMP y protege las suscripciones por paciente.
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
    private static final Pattern PATIENT_TOPIC = Pattern.compile("^/topic/patients/(\\d+)/(caregiver|doctor)$");

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ClinicalAccessService clinicalAccessService;

    public StompAuthChannelInterceptor(JwtService jwtService,
                                       UserDetailsService userDetailsService,
                                       ClinicalAccessService clinicalAccessService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.clinicalAccessService = clinicalAccessService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            String token = extractToken(accessor);
            if (token == null) {
                LOGGER.warn("STOMP CONNECT rejected: no Authorization header");
                throw new org.springframework.security.access.AccessDeniedException("Missing Authorization token");
            }
            try {
                String email = jwtService.extractUsername(token);
                var userDetails = userDetailsService.loadUserByUsername(email);
                if (!jwtService.isTokenValid(token, userDetails)) {
                    throw new org.springframework.security.access.AccessDeniedException("Invalid or expired token");
                }
                var auth = new UsernamePasswordAuthenticationToken(email, null, userDetails.getAuthorities());
                accessor.setUser(auth);
                if (accessor.getSessionAttributes() == null) {
                    accessor.setSessionAttributes(new java.util.HashMap<>());
                }
                accessor.getSessionAttributes().put("userEmail", email);
                accessor.getSessionAttributes().put("userAuthorities", userDetails.getAuthorities());
                LOGGER.debug("STOMP CONNECT authenticated email={}", email);
            } catch (Exception exception) {
                LOGGER.warn("STOMP CONNECT rejected: {}", exception.getMessage());
                throw new org.springframework.security.access.AccessDeniedException("Invalid token: " + exception.getMessage());
            }
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            if (destination == null) {
                return message;
            }
            Matcher matcher = PATIENT_TOPIC.matcher(destination);
            if (matcher.matches()) {
                long topicPatientId = Long.parseLong(matcher.group(1));
                validateSubscribeAccess(accessor, topicPatientId, destination);
            }
        }

        return message;
    }

    private void validateSubscribeAccess(StompHeaderAccessor accessor, long topicPatientId, String destination) {
        var principal = accessor.getUser();
        if (principal == null) {
            LOGGER.warn("STOMP SUBSCRIBE rejected: unauthenticated for destination={}", destination);
            throw new org.springframework.security.access.AccessDeniedException("Authentication required to subscribe");
        }

        try {
            // Reusa las reglas clinicas REST para no abrir topics por paciente sin vinculo activo.
            clinicalAccessService.requirePatientAccess(principal.getName(), topicPatientId);
        } catch (RuntimeException exception) {
            LOGGER.warn("STOMP SUBSCRIBE rejected: principal={} destination={} reason={}",
                    principal.getName(), destination, exception.getMessage());
            throw new org.springframework.security.access.AccessDeniedException(
                    "Cannot subscribe to the requested patient's topic", exception);
        }
        LOGGER.debug("STOMP SUBSCRIBE allowed destination={} principal={}", destination, principal.getName());
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        String tokenHeader = accessor.getFirstNativeHeader("token");
        return tokenHeader;
    }
}
