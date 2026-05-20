package com.neurolive.neuro_live_backend.domain.user;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Verifica la validacion del modelo de dominio AuditLog al registrar eventos.
class AuditLogTest {

    @Test
    void record_shouldPopulateAllFieldsCorrectly() {
        AuditLog log = new AuditLog();
        LocalDateTime ts = LocalDateTime.of(2026, 1, 15, 10, 30);
        log.record(1L, "READ_DATA", 2L, "192.168.1.1", ts);

        assertEquals(1L, log.getUserId());
        assertEquals("READ_DATA", log.getAction());
        assertEquals(2L, log.getTargetPatientId());
        assertEquals("192.168.1.1", log.getIpOrigin());
        assertEquals(ts, log.getTimestamp());
    }

    @Test
    void record_shouldRejectNullUserId() {
        AuditLog log = new AuditLog();
        assertThrows(IllegalArgumentException.class,
                () -> log.record(null, "LOGIN", 1L, "127.0.0.1"));
    }

    @Test
    void record_shouldRejectZeroUserId() {
        AuditLog log = new AuditLog();
        assertThrows(IllegalArgumentException.class,
                () -> log.record(0L, "LOGIN", 1L, "127.0.0.1"));
    }

    @Test
    void record_shouldRejectBlankAction() {
        AuditLog log = new AuditLog();
        assertThrows(IllegalArgumentException.class,
                () -> log.record(1L, "  ", 1L, "127.0.0.1"));
    }

    @Test
    void record_shouldRejectNullTargetPatientId() {
        AuditLog log = new AuditLog();
        assertThrows(IllegalArgumentException.class,
                () -> log.record(1L, "ACTION", null, "127.0.0.1"));
    }

    @Test
    void record_shouldRejectBlankIpOrigin() {
        AuditLog log = new AuditLog();
        assertThrows(IllegalArgumentException.class,
                () -> log.record(1L, "ACTION", 2L, "  "));
    }

    @Test
    void record_shouldTrimActionBeforeStoring() {
        AuditLog log = new AuditLog();
        log.record(1L, "  LOGIN  ", 1L, "127.0.0.1");
        assertEquals("LOGIN", log.getAction());
    }

    @Test
    void record_shouldDefaultTimestampToNowWhenNull() {
        AuditLog log = new AuditLog();
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        log.record(1L, "ACTION", 2L, "127.0.0.1", null);
        assertNotNull(log.getTimestamp());
        assertNotNull(log.getTimestamp().isAfter(before));
    }
}
