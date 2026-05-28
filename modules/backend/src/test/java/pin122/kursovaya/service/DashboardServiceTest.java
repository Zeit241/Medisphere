package pin122.kursovaya.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pin122.kursovaya.dto.DashboardDto;
import pin122.kursovaya.dto.DoctorDto;
import pin122.kursovaya.model.*;
import pin122.kursovaya.repository.AppointmentRepository;
import pin122.kursovaya.repository.PatientRepository;
import pin122.kursovaya.repository.SpecializationRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private SpecializationRepository specializationRepository;
    @Mock private DoctorService doctorService;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @InjectMocks private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dashboardService, "directusPublicUrl", "http://localhost:8055");
    }

    private Object[] specRow(Specialization spec, long count) {
        return new Object[]{spec, count};
    }

    @Test
    void getDashboardData_aggregatesSections() {
        Specialization spec = new Specialization();
        spec.setId(1L);
        spec.setCode("therapy");
        spec.setName("Therapy");
        Object[] row = specRow(spec, 3L);
        when(specializationRepository.findTopSpecializationsByDoctorCount())
                .thenReturn(List.<Object[]>of(row));
        when(doctorService.getAllDoctors(10, 0, "rating", "desc"))
                .thenReturn(List.of(new DoctorDto()));
        when(patientRepository.findByUserId(5L)).thenReturn(Optional.empty());

        DashboardDto dto = dashboardService.getDashboardData(5L);

        assertEquals(1, dto.getTopSpecializations().size());
        assertEquals(1, dto.getTopDoctors().size());
        assertNotNull(dto.getUpcomingAppointments());
    }

    @Test
    void getDashboardData_nullUserId() {
        when(specializationRepository.findTopSpecializationsByDoctorCount()).thenReturn(Collections.emptyList());
        when(doctorService.getAllDoctors(anyInt(), anyInt(), anyString(), anyString())).thenReturn(List.of());

        DashboardDto dto = dashboardService.getDashboardData(null);

        assertTrue(dto.getUpcomingAppointments().isEmpty());
    }

    @Test
    void getDashboardData_withUpcomingAppointments() {
        when(specializationRepository.findTopSpecializationsByDoctorCount()).thenReturn(Collections.emptyList());
        when(doctorService.getAllDoctors(anyInt(), anyInt(), anyString(), anyString())).thenReturn(List.of());
        Patient patient = new Patient();
        patient.setId(2L);
        when(patientRepository.findByUserId(5L)).thenReturn(Optional.of(patient));
        Appointment appt = new Appointment();
        appt.setId(10L);
        appt.setStatus("scheduled");
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        User du = new User();
        du.setFirstName("Doc");
        du.setLastName("Tor");
        doctor.setUser(du);
        appt.setDoctor(doctor);
        when(appointmentRepository.findScheduledAppointmentsByPatient(2L))
                .thenReturn(List.of(appt));

        DashboardDto dto = dashboardService.getDashboardData(5L);

        assertEquals(1, dto.getUpcomingAppointments().size());
    }

    @Test
    void getDashboardData_limitsSpecializationsToFive() {
        Specialization spec = new Specialization();
        spec.setId(1L);
        spec.setCode("a");
        spec.setName("A");
        when(specializationRepository.findTopSpecializationsByDoctorCount())
                .thenReturn(List.<Object[]>of(
                        specRow(spec, 1L), specRow(spec, 2L), specRow(spec, 3L),
                        specRow(spec, 4L), specRow(spec, 5L), specRow(spec, 6L)
                ));
        when(doctorService.getAllDoctors(anyInt(), anyInt(), anyString(), anyString())).thenReturn(List.of());
        when(patientRepository.findByUserId(any())).thenReturn(Optional.empty());

        assertEquals(5, dashboardService.getDashboardData(1L).getTopSpecializations().size());
    }

    @Test
    void getDashboardData_topDoctorsCount() {
        when(specializationRepository.findTopSpecializationsByDoctorCount()).thenReturn(Collections.emptyList());
        when(doctorService.getAllDoctors(10, 0, "rating", "desc")).thenReturn(List.of(new DoctorDto(), new DoctorDto()));
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertEquals(2, dashboardService.getDashboardData(1L).getTopDoctors().size());
    }

    @Test
    void getDashboardData_patientNotFound_emptyAppointments() {
        when(specializationRepository.findTopSpecializationsByDoctorCount()).thenReturn(Collections.emptyList());
        when(doctorService.getAllDoctors(anyInt(), anyInt(), anyString(), anyString())).thenReturn(List.of());
        when(patientRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertTrue(dashboardService.getDashboardData(99L).getUpcomingAppointments().isEmpty());
    }

    @Test
    void getDashboardData_specializationStatsMapping() {
        Specialization spec = new Specialization();
        spec.setId(7L);
        spec.setCode("dent");
        spec.setName("Dental");
        spec.setDescription("Desc");
        when(specializationRepository.findTopSpecializationsByDoctorCount())
                .thenReturn(List.<Object[]>of(specRow(spec, 4L)));
        when(doctorService.getAllDoctors(anyInt(), anyInt(), anyString(), anyString())).thenReturn(List.of());
        when(patientRepository.findByUserId(any())).thenReturn(Optional.empty());

        var stats = dashboardService.getDashboardData(1L).getTopSpecializations().get(0);

        assertEquals(4L, stats.getDoctorCount());
        assertEquals("Dental", stats.getName());
    }

    @Test
    void getDashboardData_verifyDoctorServiceCall() {
        when(specializationRepository.findTopSpecializationsByDoctorCount()).thenReturn(Collections.emptyList());
        when(doctorService.getAllDoctors(10, 0, "rating", "desc")).thenReturn(List.of());
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.empty());

        dashboardService.getDashboardData(1L);

        verify(doctorService).getAllDoctors(10, 0, "rating", "desc");
    }
}
