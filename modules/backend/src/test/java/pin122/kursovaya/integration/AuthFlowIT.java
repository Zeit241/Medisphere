package pin122.kursovaya.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import pin122.kursovaya.dto.LoginRequest;
import pin122.kursovaya.repository.RoleRepository;
import pin122.kursovaya.repository.UserRepository;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthFlowIT extends AbstractIntegrationIT {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    private String userEmail;
    private String userPassword = "secret123";

    @BeforeEach
    void seedUser() {
        userEmail = createUser(userRepository, roleRepository, "auth@test.com", userPassword, "patient").getEmail();
    }

    @Test
    void login_success_returnsJwt() {
        LoginRequest body = new LoginRequest();
        body.setEmail(userEmail);
        body.setPassword(userPassword);

        ResponseEntity<Map> response = rest.postForEntity(
                baseUrl() + "/api/auth/login",
                json(body),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(Boolean.TRUE.equals(response.getBody().get("success")));
    }

    @Test
    void login_wrongPassword_returnsBadRequest() {
        LoginRequest body = new LoginRequest();
        body.setEmail(userEmail);
        body.setPassword("wrong");

        ResponseEntity<Map> response = rest.postForEntity(
                baseUrl() + "/api/auth/login",
                json(body),
                Map.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void login_unknownEmail_returnsBadRequest() {
        LoginRequest body = new LoginRequest();
        body.setEmail("missing@test.com");
        body.setPassword("secret123");

        ResponseEntity<Map> response = rest.postForEntity(
                baseUrl() + "/api/auth/login",
                json(body),
                Map.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        ResponseEntity<String> response = rest.getForEntity(baseUrl() + "/api/users/me", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void protectedEndpoint_withToken_returns200() {
        String token = loginToken(userEmail, userPassword);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = rest.exchange(
                baseUrl() + "/api/users/me",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private String loginToken(String email, String password) {
        LoginRequest body = new LoginRequest();
        body.setEmail(email);
        body.setPassword(password);
        ResponseEntity<Map> response = rest.postForEntity(baseUrl() + "/api/auth/login", json(body), Map.class);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        return (String) data.get("token");
    }

    private HttpEntity<?> json(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
