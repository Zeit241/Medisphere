package pin122.kursovaya.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pin122.kursovaya.model.Role;
import pin122.kursovaya.model.User;
import pin122.kursovaya.repository.RoleRepository;
import pin122.kursovaya.repository.UserRepository;
import pin122.kursovaya.utils.EncryptPassword;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
public abstract class AbstractIntegrationIT {

    private static RedisServer redisServer;
    private static final int REDIS_PORT = 6379;

    @LocalServerPort
    protected int port;

    protected TestRestTemplate rest = new TestRestTemplate();

    @BeforeAll
    static void startEmbeddedRedis() throws IOException {
        redisServer = RedisServer.newRedisServer().port(REDIS_PORT).build();
        redisServer.start();
    }

    @AfterAll
    static void stopEmbeddedRedis() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:clinic_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> REDIS_PORT);
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected Role ensureRole(RoleRepository roleRepository, String code, String name) {
        return roleRepository.findByCode(code).orElseGet(() -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            return roleRepository.save(role);
        });
    }

    protected User createUser(UserRepository userRepository, RoleRepository roleRepository,
                              String email, String password, String roleCode) {
        Role role = ensureRole(roleRepository, roleCode, roleCode);
        User user = new User();
        String uniqueEmail = email.replace("@", "+" + UUID.randomUUID().toString().substring(0, 8) + "@");
        user.setEmail(uniqueEmail);
        user.setPhone("+7900" + Math.abs(uniqueEmail.hashCode() % 1_000_0000));
        user.setPasswordHash(EncryptPassword.hashPassword(password));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setActive(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setRole(role);
        return userRepository.save(user);
    }
}
