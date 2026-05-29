package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.user.AuditLog;
import com.neurolive.neuro_live_backend.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Verifica el registro de auditoría, la paginacion y la validacion de rangos de fecha.
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository);
    }

    @Test
    void record_shouldPersistAuditEntry() {
        AuditLog saved = new AuditLog();
        saved.record(1L, "LOGIN", 1L, "127.0.0.1");
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(saved);

        AuditLog result = auditLogService.record(1L, "LOGIN", 1L, "127.0.0.1");

        assertNotNull(result);
        assertEquals("LOGIN", result.getAction());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void record_shouldStartIndependentTransactionForReadOnlyCallers() throws NoSuchMethodException {
        Method recordMethod = AuditLogService.class.getMethod(
                "record",
                Long.class,
                String.class,
                Long.class,
                String.class);

        Transactional transactional = recordMethod.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    @Test
    void record_shouldSealEntryWithPreviousAuditHash() {
        AuditLog previous = auditLog(1L, 7L, "LOGIN", 7L, "127.0.0.1",
                LocalDateTime.of(2026, 5, 20, 8, 0), null);
        previous.sealWithChain(null);
        when(auditLogRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(previous));
        doAnswer(invocation -> invocation.getArgument(0))
                .when(auditLogRepository).save(any(AuditLog.class));

        AuditLog result = auditLogService.record(8L, "READ_BASELINE", 9L, "10.0.0.8");

        assertEquals(previous.getEntryHash(), result.getPreviousHash());
        assertNotNull(result.getEntryHash());
        assertTrue(result.verifyHash());
    }

    @Test
    void verifyChain_shouldRejectUnsealedEntryAfterChainStarted() {
        AuditLog first = auditLog(1L, 7L, "LOGIN", 7L, "127.0.0.1",
                LocalDateTime.of(2026, 5, 20, 8, 0), null);
        first.sealWithChain(null);
        AuditLog broken = auditLog(2L, 8L, "READ_BASELINE", 9L, "10.0.0.8",
                LocalDateTime.of(2026, 5, 20, 8, 1), first.getEntryHash());
        broken.sealWithChain(first.getEntryHash());
        setField(AuditLog.class, broken, "entryHash", null);
        when(auditLogRepository.findAllByOrderByIdAsc()).thenReturn(List.of(first, broken));

        AuditLogService.ChainVerificationResult result = auditLogService.verifyChain();

        assertFalse(result.valid());
        assertEquals(2L, result.brokenAtId());
    }

    @Test
    void verifyChain_shouldAllowLeadingLegacyEntriesWithoutHash() {
        AuditLog legacy = auditLog(1L, 7L, "LOGIN", 7L, "127.0.0.1",
                LocalDateTime.of(2026, 5, 20, 8, 0), null);
        AuditLog sealed = auditLog(2L, 8L, "READ_BASELINE", 9L, "10.0.0.8",
                LocalDateTime.of(2026, 5, 20, 8, 1), null);
        sealed.sealWithChain(null);
        when(auditLogRepository.findAllByOrderByIdAsc()).thenReturn(List.of(legacy, sealed));

        AuditLogService.ChainVerificationResult result = auditLogService.verifyChain();

        assertTrue(result.valid());
        assertEquals(2, result.totalEntries());
    }

    @Test
    void getByUser_shouldDelegateToRepository() {
        AuditLog entry = new AuditLog();
        entry.record(1L, "READ_DATA", 2L, "127.0.0.1");
        when(auditLogRepository.findAllByUserIdOrderByTimestampDesc(1L)).thenReturn(List.of(entry));

        List<AuditLog> result = auditLogService.getByUser(1L);

        assertEquals(1, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getByUser_withPagination_shouldClampSizeBelow1To1() {
        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findAllByUserIdOrderByTimestampDesc(eq(1L), any()))
                .thenReturn(emptyPage);

        auditLogService.getByUser(1L, 0, 0);

        ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(auditLogRepository).findAllByUserIdOrderByTimestampDesc(eq(1L), captor.capture());
        assertEquals(1, captor.getValue().getPageSize());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getByUser_withPagination_shouldClampSizeAbove200To200() {
        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findAllByUserIdOrderByTimestampDesc(eq(1L), any()))
                .thenReturn(emptyPage);

        auditLogService.getByUser(1L, 0, 500);

        ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(auditLogRepository).findAllByUserIdOrderByTimestampDesc(eq(1L), captor.capture());
        assertEquals(200, captor.getValue().getPageSize());
    }

    @Test
    void getByDateRange_shouldThrowWhenStartIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> auditLogService.getByDateRange(null, LocalDateTime.now()));
    }

    @Test
    void getByDateRange_shouldThrowWhenEndIsBeforeStart() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.minusHours(1);

        assertThrows(IllegalArgumentException.class,
                () -> auditLogService.getByDateRange(start, end));
    }

    @Test
    void getByDateRange_shouldDelegateToRepositoryWithValidRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        AuditLog entry = new AuditLog();
        entry.record(1L, "EXPORT_CSV", 2L, "10.0.0.1");
        when(auditLogRepository.findAllByTimestampBetweenOrderByTimestampDesc(start, end))
                .thenReturn(List.of(entry));

        List<AuditLog> result = auditLogService.getByDateRange(start, end);

        assertEquals(1, result.size());
    }

    @Test
    void getByPatient_shouldDelegateToRepository() {
        AuditLog entry = new AuditLog();
        entry.record(1L, "VIEW_BIOMETRIC", 2L, "192.168.1.5");
        when(auditLogRepository.findAllByTargetPatientIdOrderByTimestampDesc(2L))
                .thenReturn(List.of(entry));

        List<AuditLog> result = auditLogService.getByPatient(2L);

        assertEquals(1, result.size());
        assertEquals("VIEW_BIOMETRIC", result.getFirst().getAction());
        verify(auditLogRepository).findAllByTargetPatientIdOrderByTimestampDesc(2L);
    }

    @Test
    void getByDateRange_shouldThrowWhenEndIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> auditLogService.getByDateRange(LocalDateTime.now().minusDays(1), null));
    }

    private AuditLog auditLog(Long id,
                              Long userId,
                              String action,
                              Long targetPatientId,
                              String ipOrigin,
                              LocalDateTime timestamp,
                              String previousHash) {
        AuditLog auditLog = new AuditLog();
        auditLog.record(userId, action, targetPatientId, ipOrigin, timestamp);
        setField(AuditLog.class, auditLog, "id", id);
        setField(AuditLog.class, auditLog, "previousHash", previousHash);
        return auditLog;
    }

    private void setField(Class<?> owner, Object target, String fieldName, Object value) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to set field " + fieldName, e);
        }
    }
}
