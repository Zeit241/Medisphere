package pin122.kursovaya.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pin122.kursovaya.dto.DiagnosisDto;
import pin122.kursovaya.model.Diagnosis;
import pin122.kursovaya.repository.DiagnosisRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiagnosisServiceTest {

    @Mock private DiagnosisRepository diagnosisRepository;
    @InjectMocks private DiagnosisService diagnosisService;

    private Diagnosis diagnosis() {
        Diagnosis d = new Diagnosis();
        d.setId(1L);
        d.setCode("J06");
        d.setName("Cold");
        return d;
    }

    @Test
    void getAllDiagnoses() {
        when(diagnosisRepository.findAll()).thenReturn(List.of(diagnosis()));

        List<DiagnosisDto> list = diagnosisService.getAllDiagnoses();

        assertEquals(1, list.size());
    }

    @Test
    void getDiagnosisById() {
        when(diagnosisRepository.findById(1L)).thenReturn(Optional.of(diagnosis()));

        assertTrue(diagnosisService.getDiagnosisById(1L).isPresent());
    }

    @Test
    void getDiagnosisByCode() {
        when(diagnosisRepository.findByCode("J06")).thenReturn(Optional.of(diagnosis()));

        assertEquals("Cold", diagnosisService.getDiagnosisByCode("J06").get().getName());
    }

    @Test
    void saveDiagnosis() {
        Diagnosis d = diagnosis();
        when(diagnosisRepository.save(d)).thenReturn(d);

        assertEquals("J06", diagnosisService.saveDiagnosis(d).getCode());
    }

    @Test
    void deleteDiagnosis() {
        diagnosisService.deleteDiagnosis(1L);

        verify(diagnosisRepository).deleteById(1L);
    }

    @Test
    void getDiagnosisById_notFound() {
        when(diagnosisRepository.findById(2L)).thenReturn(Optional.empty());

        assertTrue(diagnosisService.getDiagnosisById(2L).isEmpty());
    }
}
