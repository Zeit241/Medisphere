package pin122.kursovaya.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pin122.kursovaya.dto.DailyReportDto;
import pin122.kursovaya.model.Appointment;
import pin122.kursovaya.model.Doctor;
import pin122.kursovaya.model.Patient;
import pin122.kursovaya.model.User;
import pin122.kursovaya.repository.AppointmentRepository;
import pin122.kursovaya.repository.DoctorRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private DoctorRepository doctorRepository;
    @InjectMocks private ReportService reportService;

    @Test
    void getAllAppointmentsByDate_countsStatuses() {
        LocalDate date = LocalDate.of(2026, 3, 1);
        when(appointmentRepository.findAllByDate(any(), any()))
                .thenReturn(List.of(
                        appointment("scheduled"),
                        appointment("completed"),
                        appointment("cancelled")
                ));

        DailyReportDto report = reportService.getAllAppointmentsByDate(date);

        assertEquals(3, report.getTotalAppointments());
        assertEquals(1, report.getScheduledCount());
        assertEquals(1, report.getCompletedCount());
        assertEquals(1, report.getCancelledCount());
    }

    @Test
    void getAllAppointmentsByDate_emptyDay() {
        when(appointmentRepository.findAllByDate(any(), any())).thenReturn(List.of());

        assertEquals(0, reportService.getAllAppointmentsByDate(LocalDate.now()).getTotalAppointments());
    }

    @Test
    void getAppointmentsByDoctorAndDate_withDoctorName() {
        LocalDate date = LocalDate.of(2026, 3, 3);
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        User user = new User();
        user.setFirstName("Ann");
        user.setLastName("Doc");
        doctor.setUser(user);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorIdAndDateForReport(eq(1L), any(), any()))
                .thenReturn(List.of(appointment("scheduled")));

        DailyReportDto report = reportService.getAppointmentsByDoctorAndDate(1L, date);

        assertEquals(1, report.getTotalAppointments());
        assertNotNull(report.getDoctorDisplayName());
    }

    @Test
    void getAppointmentsByDoctorAndDate_unknownDoctor() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        when(appointmentRepository.findByDoctorIdAndDateForReport(eq(99L), any(), any()))
                .thenReturn(List.of());

        assertEquals(0, reportService.getAppointmentsByDoctorAndDate(99L, LocalDate.now()).getTotalAppointments());
    }

    @Test
    void getAllAppointmentsByDate_noShowCounted() {
        when(appointmentRepository.findAllByDate(any(), any()))
                .thenReturn(List.of(appointment("no_show")));

        assertEquals(1, reportService.getAllAppointmentsByDate(LocalDate.now()).getNoShowCount());
    }

    @Test
    void getAllAppointmentsByDate_confirmedAsScheduled() {
        when(appointmentRepository.findAllByDate(any(), any()))
                .thenReturn(List.of(appointment("confirmed")));

        assertEquals(1, reportService.getAllAppointmentsByDate(LocalDate.now()).getScheduledCount());
    }

    @Test
    void getAppointmentsByDateRange_countsStatuses() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 5);
        when(appointmentRepository.findAllByDateRange(any(), any()))
                .thenReturn(List.of(appointment("completed"), appointment("scheduled")));

        DailyReportDto report = reportService.getAppointmentsByDateRange(start, end);

        assertEquals(2, report.getTotalAppointments());
        assertEquals(1, report.getCompletedCount());
        assertEquals(1, report.getScheduledCount());
    }

    @Test
    void getAppointmentsByDoctorAndDateRange_withDoctorName() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 2);
        Doctor doctor = new Doctor();
        doctor.setId(5L);
        User user = new User();
        user.setFirstName("Ivan");
        user.setLastName("Petrov");
        doctor.setUser(user);
        when(doctorRepository.findById(5L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorIdAndDateRangeForReport(eq(5L), any(), any()))
                .thenReturn(List.of(appointment("no_show")));

        DailyReportDto report = reportService.getAppointmentsByDoctorAndDateRange(5L, start, end);

        assertEquals(1, report.getNoShowCount());
        assertNotNull(report.getDoctorDisplayName());
        assertEquals(5L, report.getDoctorId());
    }

    @Test
    void mapToReportDto_enrichesPatientAndRoom() {
        when(appointmentRepository.findAllByDate(any(), any()))
                .thenReturn(List.of(fullAppointment()));

        DailyReportDto report = reportService.getAllAppointmentsByDate(LocalDate.now());

        assertEquals(1, report.getAppointments().size());
        assertEquals("A P M", report.getAppointments().get(0).getPatientFullName());
        assertEquals("101", report.getAppointments().get(0).getRoomNumber());
    }

    private Appointment fullAppointment() {
        Appointment a = appointment("completed");
        a.getPatient().setGender((short) 1);
        a.getPatient().setBirthDate(LocalDate.of(1990, 1, 1));
        a.getPatient().getUser().setMiddleName("M");
        a.getPatient().getUser().setPhone("+7999");
        a.getPatient().getUser().setEmail("p@example.com");
        pin122.kursovaya.model.Room room = new pin122.kursovaya.model.Room();
        room.setId(10L);
        room.setCode("101");
        a.setRoom(room);
        return a;
    }

    private Appointment appointment(String status) {
        Appointment a = new Appointment();
        a.setId(1L);
        a.setStatus(status);
        a.setStartTime(OffsetDateTime.now(ZoneOffset.UTC));
        a.setEndTime(a.getStartTime().plusMinutes(30));
        Patient patient = new Patient();
        patient.setId(2L);
        User user = new User();
        user.setFirstName("P");
        user.setLastName("A");
        patient.setUser(user);
        a.setPatient(patient);
        Doctor doctor = new Doctor();
        doctor.setId(3L);
        User du = new User();
        du.setFirstName("D");
        du.setLastName("R");
        doctor.setUser(du);
        a.setDoctor(doctor);
        return a;
    }
}
