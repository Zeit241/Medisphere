package pin122.kursovaya.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pin122.kursovaya.dto.SpecializationDto;
import pin122.kursovaya.model.Specialization;
import pin122.kursovaya.repository.SpecializationRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecializationServiceTest {

    @Mock private SpecializationRepository specializationRepository;
    @InjectMocks private SpecializationService specializationService;

    private Specialization spec() {
        Specialization s = new Specialization();
        s.setId(1L);
        s.setCode("cardio");
        s.setName("Кардиология");
        return s;
    }

    @Test
    void getAllSpecializations() {
        when(specializationRepository.findAll()).thenReturn(List.of(spec()));

        assertEquals(1, specializationService.getAllSpecializations().size());
    }

    @Test
    void getSpecializationById_found() {
        when(specializationRepository.findById(1L)).thenReturn(Optional.of(spec()));

        Optional<SpecializationDto> dto = specializationService.getSpecializationById(1L);

        assertTrue(dto.isPresent());
        assertEquals("cardio", dto.get().getCode());
    }

    @Test
    void getSpecializationById_notFound() {
        when(specializationRepository.findById(2L)).thenReturn(Optional.empty());

        assertTrue(specializationService.getSpecializationById(2L).isEmpty());
    }

    @Test
    void getSpecializationByCode() {
        when(specializationRepository.findByCode("cardio")).thenReturn(Optional.of(spec()));

        assertTrue(specializationService.getSpecializationByCode("cardio").isPresent());
    }

    @Test
    void saveSpecialization() {
        Specialization s = spec();
        when(specializationRepository.save(s)).thenReturn(s);

        SpecializationDto dto = specializationService.saveSpecialization(s);

        assertEquals("Кардиология", dto.getName());
    }

    @Test
    void deleteSpecialization() {
        specializationService.deleteSpecialization(1L);

        verify(specializationRepository).deleteById(1L);
    }
}
