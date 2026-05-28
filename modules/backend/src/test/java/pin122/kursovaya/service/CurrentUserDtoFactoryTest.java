package pin122.kursovaya.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pin122.kursovaya.dto.CurrentUserDto;
import pin122.kursovaya.model.Doctor;
import pin122.kursovaya.model.Patient;
import pin122.kursovaya.model.Role;
import pin122.kursovaya.model.User;
import pin122.kursovaya.repository.DoctorRepository;
import pin122.kursovaya.repository.PatientRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserDtoFactoryTest {

    @Mock private DoctorRepository doctorRepository;
    @Mock private PatientRepository patientRepository;
    @InjectMocks private CurrentUserDtoFactory factory;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(factory, "directusPublicUrl", "http://localhost:8055");
    }

    @Test
    void build_nullUser_returnsNull() {
        assertNull(factory.build(null));
    }

    @Test
    void build_patientRole_enrichesPatientId() {
        User user = userWithRole("patient");
        Patient patient = new Patient();
        patient.setId(7L);
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(patient));

        CurrentUserDto dto = factory.build(user);

        assertEquals(7L, dto.getPatientId());
    }

    @Test
    void build_doctorRole_enrichesDoctorId() {
        User user = userWithRole("doctor");
        Doctor doctor = new Doctor();
        doctor.setId(3L);
        when(doctorRepository.findByUserId(1L)).thenReturn(Optional.of(doctor));

        CurrentUserDto dto = factory.build(user);

        assertEquals(3L, dto.getDoctorId());
    }

    @Test
    void build_withoutRole_skipsEnrichment() {
        User user = new User();
        user.setId(1L);
        user.setEmail("x@test.com");

        CurrentUserDto dto = factory.build(user);

        assertNull(dto.getDoctorId());
        assertNull(dto.getPatientId());
    }

    private User userWithRole(String code) {
        User user = new User();
        user.setId(1L);
        user.setEmail("u@test.com");
        Role role = new Role();
        role.setCode(code);
        user.setRole(role);
        return user;
    }
}
