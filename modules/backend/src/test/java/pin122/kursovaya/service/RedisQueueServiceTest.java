package pin122.kursovaya.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import pin122.kursovaya.dto.WebSocketSessionData;
import pin122.kursovaya.repository.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisQueueServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private DefaultRedisScript<Long> removeAndShiftScript;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private ZSetOperations<String, String> zSetOps;
    @Mock private SetOperations<String, String> setOps;
    @InjectMocks private RedisQueueService redisQueueService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOps);
    }

    @Test
    void generateSessionId_notBlank() {
        assertNotNull(redisQueueService.generateSessionId());
    }

    @Test
    void saveSession_writesJson() {
        WebSocketSessionData data = new WebSocketSessionData(
                "s1", 1L, 2L, "u@test.com", List.of(1L), LocalDateTime.now()
        );
        redisQueueService.saveSession("s1", data);
        verify(valueOps).set(startsWith("ws:session:"), anyString());
        verify(setOps).add(eq("ws:sessions:active"), eq("s1"));
    }

    @Test
    void getSession_missingReturnsNull() {
        when(valueOps.get("ws:session:missing")).thenReturn(null);
        assertNull(redisQueueService.getSession("missing"));
    }

    @Test
    void getSession_roundTrip() {
        WebSocketSessionData data = new WebSocketSessionData(
                "s2", 1L, 2L, "u@test.com", List.of(), LocalDateTime.now()
        );
        redisQueueService.saveSession("s2", data);
        when(valueOps.get("ws:session:s2")).thenReturn(
                "{\"sessionId\":\"s2\",\"userId\":1,\"patientId\":2,\"email\":\"u@test.com\",\"appointmentIds\":[],\"connectedAt\":\"2026-01-01T10:00:00\"}"
        );
        WebSocketSessionData loaded = redisQueueService.getSession("s2");
        assertNotNull(loaded);
        assertEquals("s2", loaded.getSessionId());
    }

    @Test
    void deleteSession_removesKeys() {
        WebSocketSessionData data = new WebSocketSessionData(
                "s3", 1L, 4L, "d@test.com", List.of(), LocalDateTime.now()
        );
        when(valueOps.get("ws:session:s3")).thenReturn(
                "{\"sessionId\":\"s3\",\"userId\":1,\"patientId\":4,\"email\":\"d@test.com\",\"appointmentIds\":[],\"connectedAt\":\"2026-01-01T10:00:00\"}"
        );
        redisQueueService.deleteSession("s3");
        verify(redisTemplate).delete("ws:session:s3");
    }

    @Test
    void addToQueue_addsZSetMember() {
        LocalDate day = LocalDate.of(2026, 1, 15);
        redisQueueService.addToQueue(10L, 20L, 1, day);
        verify(zSetOps).add(contains("queue:doctor:20:"), eq("patient:10"), eq(1.0));
    }

    @Test
    void removeFromQueue_executesScript() {
        when(redisTemplate.execute(eq(removeAndShiftScript), anyList(), any())).thenReturn(1L);
        assertTrue(redisQueueService.removeFromQueue(10L, 20L, LocalDate.now()));
    }

    @Test
    void clearQueue_deletesKey() {
        redisQueueService.clearQueue(20L, LocalDate.now());
        verify(redisTemplate).delete(contains("queue:doctor:20:"));
    }

    @Test
    void hasActiveSessions_trueWhenSetNotEmpty() {
        when(setOps.size("patient:sessions:5")).thenReturn(1L);
        assertTrue(redisQueueService.hasActiveSessions(5L));
    }

    @Test
    void hasActiveSessions_falseWhenEmpty() {
        when(setOps.size("patient:sessions:6")).thenReturn(0L);
        assertFalse(redisQueueService.hasActiveSessions(6L));
    }

    @Test
    void getAllActiveSessions_emptyWhenNoSessions() {
        when(setOps.members("ws:sessions:active")).thenReturn(Set.of());
        assertTrue(redisQueueService.getAllActiveSessions().isEmpty());
    }

    @Test
    void notifyQueueUpdated_sendsToTopic() {
        redisQueueService.notifyQueueUpdated(99L, LocalDate.now());
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void notifyUserQueueUpdate_sendsToUser() {
        redisQueueService.notifyUserQueueUpdate("user@test.com", List.of());
        verify(messagingTemplate).convertAndSendToUser(eq("user@test.com"), eq("/queue/user"), any(Object.class));
    }

    @Test
    void getQueueSize_delegatesToZCard() {
        when(zSetOps.zCard(anyString())).thenReturn(0L);
        assertEquals(0L, redisQueueService.getQueueSize(1L, LocalDate.now()));
    }

    @Test
    void isPatientNextInQueue_falseWhenNoPosition() {
        when(zSetOps.score(anyString(), eq("patient:1"))).thenReturn(null);
        assertFalse(redisQueueService.isPatientNextInQueue(1L, 2L, LocalDate.now()));
    }
}
