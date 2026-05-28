package pin122.kursovaya.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pin122.kursovaya.model.Doctor;
import pin122.kursovaya.model.Patient;
import pin122.kursovaya.model.QueueEntry;
import pin122.kursovaya.repository.AppointmentRepository;
import pin122.kursovaya.repository.DoctorRepository;
import pin122.kursovaya.repository.PatientRepository;
import pin122.kursovaya.repository.QueueEntryRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock private QueueEntryRepository queueEntryRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @InjectMocks private QueueService queueService;

    @Test
    void getQueueByDoctor_mapsEntries() {
        QueueEntry entry = new QueueEntry();
        entry.setId(1L);
        entry.setPosition(0);
        when(queueEntryRepository.findByDoctorIdOrderByPositionAsc(5L)).thenReturn(List.of(entry));

        assertEquals(1, queueService.getQueueByDoctor(5L).size());
    }

    @Test
    void addPatientToQueue_assignsNextPosition() {
        Patient patient = new Patient();
        patient.setId(1L);
        Doctor doctor = new Doctor();
        doctor.setId(2L);
        when(queueEntryRepository.findByPatientIdAndDoctorId(1L, 2L)).thenReturn(Optional.empty());
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(queueEntryRepository.findMaxPositionByDoctorId(2L)).thenReturn(1);
        when(queueEntryRepository.save(any(QueueEntry.class))).thenAnswer(i -> {
            QueueEntry e = i.getArgument(0);
            e.setId(10L);
            return e;
        });

        assertEquals(2, queueService.addPatientToQueue(1L, 2L, null).getPosition());
    }

    @Test
    void addPatientToQueue_duplicateThrows() {
        when(queueEntryRepository.findByPatientIdAndDoctorId(1L, 2L))
                .thenReturn(Optional.of(new QueueEntry()));

        assertThrows(IllegalStateException.class,
                () -> queueService.addPatientToQueue(1L, 2L, null));
    }

    @Test
    void isPatientNextInQueue_firstPosition() {
        QueueEntry entry = new QueueEntry();
        entry.setPosition(0);
        when(queueEntryRepository.findByPatientIdAndDoctorId(1L, 2L)).thenReturn(Optional.of(entry));

        assertTrue(queueService.isPatientNextInQueue(1L, 2L));
    }

    @Test
    void removeFromQueueAndShift_returnsFalseWhenMissing() {
        when(queueEntryRepository.findByPatientIdAndDoctorId(1L, 2L)).thenReturn(Optional.empty());

        assertFalse(queueService.removeFromQueueAndShift(1L, 2L));
    }

    @Test
    void deleteQueueEntry_delegates() {
        queueService.deleteQueueEntry(9L);
        verify(queueEntryRepository).deleteById(9L);
    }
}
