package pin122.kursovaya.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import pin122.kursovaya.dto.AuthResponse;
import pin122.kursovaya.dto.CurrentUserDto;
import pin122.kursovaya.dto.LoginRequest;
import pin122.kursovaya.model.Role;
import pin122.kursovaya.model.User;
import pin122.kursovaya.repository.UserRepository;
import pin122.kursovaya.utils.EncryptPassword;
import pin122.kursovaya.utils.JwtTokenProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - тесты аутентификации")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private CurrentUserDtoFactory currentUserDtoFactory;
    @InjectMocks private AuthService authService;

    private User user;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setPasswordHash(EncryptPassword.hashPassword("secret"));
        Role role = new Role();
        role.setCode("patient");
        user.setRole(role);
        userDetails = org.springframework.security.core.userdetails.User
                .withUsername("user@test.com").password("x").roles("PATIENT").build();
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest req = loginRequest("user@test.com", "secret");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn("jwt-token");
        when(userRepository.findByEmailWithPatientAndDoctor("user@test.com")).thenReturn(user);
        when(currentUserDtoFactory.build(user)).thenReturn(new CurrentUserDto());

        AuthResponse response = authService.login(req);

        assertEquals("jwt-token", response.getToken());
        assertEquals("patient", response.getRoleCode());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_userWithoutRole_returnsNullRoleCode() {
        user.setRole(null);
        LoginRequest req = loginRequest("user@test.com", "secret");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn("jwt-token");
        when(userRepository.findByEmailWithPatientAndDoctor("user@test.com")).thenReturn(user);
        when(currentUserDtoFactory.build(user)).thenReturn(new CurrentUserDto());

        AuthResponse response = authService.login(req);

        assertNull(response.getRoleCode());
    }

    @Test
    void authenticate_validCredentials_returnsUser() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(user);

        User result = authService.authenticate("user@test.com", "secret");

        assertNotNull(result);
        assertEquals("user@test.com", result.getEmail());
    }

    @Test
    void authenticate_wrongPassword_returnsNull() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(user);

        assertNull(authService.authenticate("user@test.com", "wrong"));
    }

    @Test
    void authenticate_unknownEmail_returnsNull() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(null);

        assertNull(authService.authenticate("missing@test.com", "secret"));
    }

    @Test
    void login_includesCurrentUserDto() {
        LoginRequest req = loginRequest("user@test.com", "secret");
        CurrentUserDto dto = new CurrentUserDto();
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn("jwt-token");
        when(userRepository.findByEmailWithPatientAndDoctor("user@test.com")).thenReturn(user);
        when(currentUserDtoFactory.build(user)).thenReturn(dto);

        AuthResponse response = authService.login(req);

        assertSame(dto, response.getUser());
    }

    @Test
    void login_successMessage() {
        LoginRequest req = loginRequest("user@test.com", "secret");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn("jwt-token");
        when(userRepository.findByEmailWithPatientAndDoctor("user@test.com")).thenReturn(user);
        when(currentUserDtoFactory.build(user)).thenReturn(new CurrentUserDto());

        AuthResponse response = authService.login(req);

        assertEquals("Успешный вход в систему", response.getMessage());
    }

    @Test
    void authenticate_emptyPassword_returnsNull() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(user);

        assertNull(authService.authenticate("user@test.com", ""));
    }

    @Test
    void login_returnsEmailInResponse() {
        LoginRequest req = loginRequest("user@test.com", "secret");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn("jwt-token");
        when(userRepository.findByEmailWithPatientAndDoctor("user@test.com")).thenReturn(user);
        when(currentUserDtoFactory.build(user)).thenReturn(new CurrentUserDto());

        assertEquals("user@test.com", authService.login(req).getEmail());
    }

    @Test
    void authenticate_nullUserPasswordHash_returnsNull() {
        user.setPasswordHash(null);
        when(userRepository.findByEmail("user@test.com")).thenReturn(user);

        assertNull(authService.authenticate("user@test.com", "secret"));
    }
}
