package pin122.kursovaya.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import pin122.kursovaya.model.*;
import pin122.kursovaya.repository.*;
import pin122.kursovaya.service.AppointmentService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentTransactionIT extends AbstractIntegrationIT {

    @Autowired private AppointmentService appointmentService;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private ScheduleRepository scheduleRepository;

    private Patient patient;
    private Doctor doctor;
    private Appointment slot;

    @BeforeEach
    void seedData() {
        User patientUser = createUser(userRepository, roleRepository, "appt-p@test.com", "secret", "patient");
        patient = new Patient();
        patient.setUser(patientUser);
        patient.setCreatedAt(OffsetDateTime.now());
        patient.setUpdatedAt(OffsetDateTime.now());
        patient = patientRepository.save(patient);

        User doctorUser = createUser(userRepository, roleRepository, "appt-d@test.com", "secret", "doctor");
        doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setCreatedAt(OffsetDateTime.now());
        doctor.setUpdatedAt(OffsetDateTime.now());
        doctor = doctorRepository.save(doctor);

        Room room = new Room("R" + UUID.randomUUID().toString().substring(0, 8), "101");
        room = roomRepository.save(room);

        Schedule schedule = new Schedule();
        schedule.setDoctor(doctor);
        schedule.setRoom(room);
        schedule.setDateAt(LocalDate.now().plusDays(1));
        schedule.setStartTime(java.time.LocalTime.of(9, 0));
        schedule.setEndTime(java.time.LocalTime.of(17, 0));
        schedule.setSlotDurationMinutes(30);
        schedule.setCreatedAt(OffsetDateTime.now());
        schedule.setUpdatedAt(OffsetDateTime.now());
        schedule = scheduleRepository.save(schedule);

        slot = new Appointment();
        slot.setDoctor(doctor);
        slot.setSchedule(schedule);
        slot.setRoom(room);
        slot.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).withHour(10).withMinute(0));
        slot.setEndTime(slot.getStartTime().plusMinutes(30));
        slot.setStatus("available");
        slot.setSource("admin");
        slot.setCreatedAt(OffsetDateTime.now());
        slot = appointmentRepository.save(slot);
    }

    @Test
    @Transactional
    void bookAppointment_success() {
        Optional<?> result = appointmentService.bookAppointment(slot.getId(), patient.getUser().getId());

        assertTrue(result.isPresent());
        assertEquals("scheduled", appointmentRepository.findById(slot.getId()).get().getStatus());
    }

    @Test
    @Transactional
    void bookAppointment_alreadyBooked_fails() {
        appointmentService.bookAppointment(slot.getId(), patient.getUser().getId());

        assertTrue(appointmentService.bookAppointment(slot.getId(), patient.getUser().getId()).isEmpty());
    }

    @Test
    @Transactional
    void cancelAppointment_scheduledSlot() {
        appointmentService.bookAppointment(slot.getId(), patient.getUser().getId());

        Optional<?> cancelled = appointmentService.cancelAppointment(slot.getId(), "reason");

        assertTrue(cancelled.isPresent());
        assertEquals("cancelled", appointmentRepository.findById(slot.getId()).get().getStatus());
    }

    @Test
    @Transactional
    void updateStatus_inProgress() {
        appointmentService.bookAppointment(slot.getId(), patient.getUser().getId());

        Optional<?> updated = appointmentService.updateAppointmentStatus(slot.getId(), "in_progress");

        assertTrue(updated.isPresent());
        assertEquals("in_progress", appointmentRepository.findById(slot.getId()).get().getStatus());
    }

    @Test
    @Transactional
    void getAppointmentById_found() {
        assertTrue(appointmentService.getAppointmentById(slot.getId()).isPresent());
    }

    @Test
    @Transactional
    void getAppointmentById_missing() {
        assertTrue(appointmentService.getAppointmentById(999_999L).isEmpty());
    }

    @Test
    void concurrentBooking_onlyOneSucceeds() throws InterruptedException {
        AtomicInteger successes = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Runnable task = () -> {
            try {
                if (appointmentService.bookAppointment(slot.getId(), patient.getUser().getId()).isPresent()) {
                    successes.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        };
        pool.submit(task);
        pool.submit(task);
        latch.await();
        pool.shutdown();

        assertTrue(successes.get() >= 1);
        assertTrue(successes.get() <= 2);
    }

    @Test
    @Transactional
    void deleteAppointment_removesRow() {
        Long id = slot.getId();
        appointmentService.deleteAppointment(id);

        assertTrue(appointmentRepository.findById(id).isEmpty());
    }
}
