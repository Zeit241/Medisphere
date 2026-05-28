package pin122.kursovaya.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import pin122.kursovaya.dto.WebSocketSessionData;
import pin122.kursovaya.service.RedisQueueService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueueRedisIT extends AbstractIntegrationIT {

    @Autowired private RedisQueueService redisQueueService;
    @Autowired private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void flushRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void generateSessionId_unique() {
        String a = redisQueueService.generateSessionId();
        String b = redisQueueService.generateSessionId();

        assertNotNull(a);
        assertNotEquals(a, b);
    }

    @Test
    void saveAndGetSession_roundTrip() {
        String sessionId = "sess-1";
        WebSocketSessionData data = new WebSocketSessionData(
                sessionId, 1L, 2L, "q@test.com", List.of(10L), LocalDateTime.now()
        );

        redisQueueService.saveSession(sessionId, data);

        WebSocketSessionData loaded = redisQueueService.getSession(sessionId);
        assertNotNull(loaded);
        assertEquals("q@test.com", loaded.getEmail());
        assertEquals(2L, loaded.getPatientId());
    }

    @Test
    void deleteSession_removesData() {
        String sessionId = "sess-del";
        redisQueueService.saveSession(sessionId, new WebSocketSessionData(
                sessionId, 1L, 3L, "del@test.com", List.of(), LocalDateTime.now()
        ));

        redisQueueService.deleteSession(sessionId);

        assertNull(redisQueueService.getSession(sessionId));
    }

    @Test
    void addToQueue_increasesSize() {
        LocalDate day = LocalDate.now();
        redisQueueService.addToQueue(100L, 200L, 1, day);

        assertEquals(1, redisQueueService.getQueueByDoctor(200L, day).size());
    }

    @Test
    void addMultiplePatients_orderedByPosition() {
        LocalDate day = LocalDate.now();
        redisQueueService.addToQueue(1L, 5L, 1, day);
        redisQueueService.addToQueue(2L, 5L, 2, day);

        var queue = redisQueueService.getQueueByDoctor(5L, day);
        assertEquals(2, queue.size());
        assertTrue(queue.get(0).getPosition() <= queue.get(1).getPosition());
    }

    @Test
    void removeFromQueue_returnsTrueWhenPresent() {
        LocalDate day = LocalDate.now();
        redisQueueService.addToQueue(7L, 8L, 1, day);

        assertTrue(redisQueueService.removeFromQueue(7L, 8L, day));
        assertTrue(redisQueueService.getQueueByDoctor(8L, day).isEmpty());
    }

    @Test
    void removeFromQueue_missingReturnsFalse() {
        assertFalse(redisQueueService.removeFromQueue(99L, 88L, LocalDate.now()));
    }

    @Test
    void clearQueue_emptiesDoctorQueue() {
        LocalDate day = LocalDate.now();
        redisQueueService.addToQueue(1L, 9L, 1, day);
        redisQueueService.addToQueue(2L, 9L, 2, day);

        redisQueueService.clearQueue(9L, day);

        assertTrue(redisQueueService.getQueueByDoctor(9L, day).isEmpty());
    }

    @Test
    void hasActiveSessions_afterSave() {
        String sessionId = "active-1";
        redisQueueService.saveSession(sessionId, new WebSocketSessionData(
                sessionId, 1L, 42L, "active@test.com", List.of(), LocalDateTime.now()
        ));

        assertTrue(redisQueueService.hasActiveSessions(42L));
    }

    @Test
    void getAllActiveSessions_listsSaved() {
        redisQueueService.saveSession("a", new WebSocketSessionData(
                "a", 1L, 10L, "a@test.com", List.of(), LocalDateTime.now()
        ));
        redisQueueService.saveSession("b", new WebSocketSessionData(
                "b", 2L, 11L, "b@test.com", List.of(), LocalDateTime.now()
        ));

        assertEquals(2, redisQueueService.getAllActiveSessions().size());
    }
}
