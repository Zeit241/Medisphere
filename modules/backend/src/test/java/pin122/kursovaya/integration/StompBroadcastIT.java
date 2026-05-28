package pin122.kursovaya.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import pin122.kursovaya.dto.WebSocketSessionData;
import pin122.kursovaya.repository.RoleRepository;
import pin122.kursovaya.repository.UserRepository;
import pin122.kursovaya.service.RedisQueueService;
import pin122.kursovaya.utils.JwtTokenProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StompBroadcastIT extends AbstractIntegrationIT {

    @Autowired private RedisQueueService redisQueueService;
    @Autowired private RedisTemplate<String, String> redisTemplate;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    @BeforeEach
    void flushRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void stompConnect_withValidJwt_succeeds() throws Exception {
        var user = createUser(userRepository, roleRepository, "stomp1@test.com", "secret", "patient");
        String token = jwtTokenProvider.generateToken(
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail()).password("x").roles("PATIENT").build()
        );

        StompSession session = connect(token);

        assertTrue(session.isConnected());
        session.disconnect();
    }

    @Test
    void stompConnect_withoutJwt_rejected() {
        assertThrows(Exception.class, () -> connect(null));
    }

    @Test
    void sessionRoundTrip_preservesAppointmentIds() {
        String id = "ws-1";
        WebSocketSessionData data = new WebSocketSessionData(
                id, 1L, 5L, "ws@test.com", List.of(1L, 2L, 3L), LocalDateTime.now()
        );
        redisQueueService.saveSession(id, data);

        WebSocketSessionData loaded = redisQueueService.getSession(id);
        assertEquals(3, loaded.getAppointmentIds().size());
    }

    @Test
    void queuePosition_afterAdd_firstIsNext() {
        LocalDate day = LocalDate.now();
        redisQueueService.addToQueue(50L, 60L, 1, day);
        redisQueueService.addToQueue(51L, 60L, 2, day);

        assertTrue(redisQueueService.isPatientNextInQueue(50L, 60L, day));
        assertFalse(redisQueueService.isPatientNextInQueue(51L, 60L, day));
    }

    @Test
    void notifyQueueUpdated_doesNotThrow() {
        LocalDate day = LocalDate.now();
        redisQueueService.addToQueue(1L, 2L, 1, day);

        assertDoesNotThrow(() -> redisQueueService.notifyQueueUpdated(2L, day));
    }

    @Test
    void clearQueueWithNotification_doesNotThrow() {
        LocalDate day = LocalDate.now();
        redisQueueService.addToQueue(3L, 4L, 1, day);

        assertDoesNotThrow(() -> redisQueueService.clearQueueWithNotification(4L, day));
        assertTrue(redisQueueService.getQueueByDoctor(4L, day).isEmpty());
    }

    @Test
    void getQueuesByPatient_withoutAppointments_empty() {
        assertTrue(redisQueueService.getQueuesByPatient(70L).isEmpty());
    }

    @Test
    void removePatientFromAllQueues_withoutAppointments_noError() {
        assertDoesNotThrow(() -> redisQueueService.removePatientFromAllQueues(90L));
    }

    @Test
    void recalculateQueueForDoctor_withoutAppointments_clearsQueue() {
        LocalDate day = LocalDate.now();
        redisQueueService.addToQueue(1L, 100L, 1, day);

        redisQueueService.recalculateQueueForDoctor(100L, day);

        assertTrue(redisQueueService.getQueueByDoctor(100L, day).isEmpty());
    }

    @Test
    void stompSubscribe_userQueue_connectOnly() throws Exception {
        var user = createUser(userRepository, roleRepository, "stomp2@test.com", "secret", "patient");
        String token = jwtTokenProvider.generateToken(
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail()).password("x").roles("PATIENT").build()
        );
        StompSession session = connect(token);
        assertTrue(session.isConnected());
        session.disconnect();
    }

    private StompSession connect(String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        CompletableFuture<StompSession> future = new CompletableFuture<>();
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        client.connectAsync(
                baseUrl().replace("http", "ws") + "/queue-websocket",
                new org.springframework.web.socket.WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        future.complete(session);
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        future.completeExceptionally(exception);
                    }
                }
        ).get(10, TimeUnit.SECONDS);
        return future.get(10, TimeUnit.SECONDS);
    }
}
