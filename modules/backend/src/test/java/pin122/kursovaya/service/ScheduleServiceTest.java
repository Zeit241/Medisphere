package pin122.kursovaya.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pin122.kursovaya.dto.ScheduleDto;
import pin122.kursovaya.model.Doctor;
import pin122.kursovaya.model.Room;
import pin122.kursovaya.model.Schedule;
import pin122.kursovaya.repository.AppointmentRepository;
import pin122.kursovaya.repository.DoctorRepository;
import pin122.kursovaya.repository.RoomRepository;
import pin122.kursovaya.repository.ScheduleRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private RoomRepository roomRepository;
    @InjectMocks private ScheduleService scheduleService;

    @Test
    void getSchedulesByDoctor_returnsMapped() {
        Schedule schedule = sampleSchedule();
        when(scheduleRepository.findByDoctorId(1L)).thenReturn(List.of(schedule));

        List<ScheduleDto> result = scheduleService.getSchedulesByDoctor(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getDoctorId());
    }

    @Test
    void getSchedulesByDoctor_withDate_filters() {
        LocalDate date = LocalDate.of(2026, 2, 1);
        when(scheduleRepository.findByDoctorIdAndDateAt(1L, date)).thenReturn(List.of(sampleSchedule()));

        assertEquals(1, scheduleService.getSchedulesByDoctor(1L, date).size());
    }

    @Test
    void getSchedulesByDoctor_nullDate_returnsAll() {
        when(scheduleRepository.findByDoctorId(1L)).thenReturn(List.of(sampleSchedule()));

        assertEquals(1, scheduleService.getSchedulesByDoctor(1L, null).size());
    }

    @Test
    void getScheduleById_found() {
        Schedule schedule = sampleSchedule();
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

        assertTrue(scheduleService.getScheduleById(5L).isPresent());
    }

    @Test
    void getScheduleById_missing() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(scheduleService.getScheduleById(99L).isEmpty());
    }

    @Test
    void deleteSchedule_delegatesToRepository() {
        scheduleService.deleteSchedule(3L);
        verify(scheduleRepository).deleteById(3L);
    }

    private Schedule sampleSchedule() {
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        Room room = new Room("R1", "Room 1");
        room.setId(2L);
        Schedule schedule = new Schedule();
        schedule.setId(5L);
        schedule.setDoctor(doctor);
        schedule.setRoom(room);
        schedule.setDateAt(LocalDate.of(2026, 2, 1));
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(17, 0));
        schedule.setSlotDurationMinutes(30);
        schedule.setCreatedAt(OffsetDateTime.now());
        schedule.setUpdatedAt(OffsetDateTime.now());
        return schedule;
    }
}
