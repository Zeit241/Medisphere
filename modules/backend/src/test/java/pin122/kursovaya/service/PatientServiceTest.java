package pin122.kursovaya.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pin122.kursovaya.dto.CreatePatientRequest;
import pin122.kursovaya.dto.PatientDto;
import pin122.kursovaya.dto.UserDto;
import pin122.kursovaya.model.Patient;
import pin122.kursovaya.model.Role;
import pin122.kursovaya.model.User;
import pin122.kursovaya.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatientService - тесты сервиса пациентов")
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private QueueEntryRepository queueEntryRepository;
    @InjectMocks private PatientService patientService;

    private Patient patient;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(10L);
        user.setEmail("p@test.com");
        user.setFirstName("Ann");
        user.setLastName("Bee");
        patient = new Patient();
        patient.setId(1L);
        patient.setUser(user);
        patient.setGender((short) 2);
    }

    @Test
    void getAllPatients_returnsMappedList() {
        when(patientRepository.findAll()).thenReturn(List.of(patient));

        List<PatientDto> result = patientService.getAllPatients();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void getPatientById_found() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        assertTrue(patientService.getPatientById(1L).isPresent());
    }

    @Test
    void getPatientById_notFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(patientService.getPatientById(99L).isEmpty());
    }

    @Test
    void createPatient_savesUserAndPatient() {
        UserDto userDto = new UserDto();
        userDto.setEmail("new@test.com");
        userDto.setPhone("+79001112233");
        userDto.setFirstName("New");
        userDto.setLastName("Patient");
        CreatePatientRequest request = new CreatePatientRequest();
        request.setUser(userDto);
        request.setBirthDate(LocalDate.of(1990, 1, 1));
        request.setGender((short) 1);
        request.setInsuranceNumber("123456789012");

        Role role = new Role();
        role.setCode("patient");
        when(roleRepository.findByCode("patient")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(20L);
            return u;
        });
        when(patientRepository.save(any(Patient.class))).thenAnswer(i -> {
            Patient p = i.getArgument(0);
            p.setId(2L);
            return p;
        });

        PatientDto dto = patientService.createPatient(request);

        assertNotNull(dto);
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    void updatePatient_updatesFields() {
        Patient update = new Patient();
        update.setGender((short) 1);
        update.setInsuranceNumber("999999999999");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));

        Optional<PatientDto> result = patientService.updatePatient(1L, update);

        assertTrue(result.isPresent());
        assertEquals((short) 1, patient.getGender());
    }

    @Test
    void deletePatient_notFound_noOp() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        patientService.deletePatient(99L);

        verify(patientRepository, never()).deleteById(any());
    }

    @Test
    void deletePatient_cascadesCleanup() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        patientService.deletePatient(1L);

        verify(queueEntryRepository).deleteByPatientId(1L);
        verify(reviewRepository).deleteByPatientId(1L);
        verify(appointmentRepository).clearPatientFromAppointments(1L);
        verify(patientRepository).deleteById(1L);
        verify(userRepository).deleteById(10L);
    }

    @Test
    void savePatient_returnsDto() {
        when(patientRepository.save(patient)).thenReturn(patient);

        PatientDto dto = patientService.savePatient(patient);

        assertEquals(1L, dto.getId());
    }

    @Test
    void updatePatient_notFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(patientService.updatePatient(99L, new Patient()).isEmpty());
    }

    @Test
    void updatePatient_updatesNestedUser() {
        User userUpdate = new User();
        userUpdate.setFirstName("Updated");
        userUpdate.setActive(false);
        Patient update = new Patient();
        update.setUser(userUpdate);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        patientService.updatePatient(1L, update);

        assertEquals("Updated", user.getFirstName());
        assertFalse(user.isActive());
    }
}
