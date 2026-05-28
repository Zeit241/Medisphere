package pin122.kursovaya.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import pin122.kursovaya.dto.ServiceDto;
import pin122.kursovaya.model.SpecializationServiceLink;
import pin122.kursovaya.model.SpecializationServiceLinkPk;
import pin122.kursovaya.repository.BookingCatalogRepository;
import pin122.kursovaya.repository.ServiceRepository;
import pin122.kursovaya.repository.SpecializationRepository;
import pin122.kursovaya.repository.SpecializationServiceLinkRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock private ServiceRepository serviceRepository;
    @Mock private BookingCatalogRepository bookingCatalogRepository;
    @Mock private SpecializationServiceLinkRepository linkRepository;
    @Mock private SpecializationRepository specializationRepository;
    @InjectMocks private ServiceService serviceService;

    private pin122.kursovaya.model.Service serviceEntity() {
        pin122.kursovaya.model.Service s = new pin122.kursovaya.model.Service();
        s.setId(1L);
        s.setName("Consultation");
        s.setCode("consult");
        return s;
    }

    @Test
    void getServicesForDoctor_empty() {
        when(bookingCatalogRepository.findServiceIdsByDoctorId(1L)).thenReturn(List.of());

        assertTrue(serviceService.getServicesForDoctor(1L).isEmpty());
    }

    @Test
    void getAllServices() {
        when(serviceRepository.findAll()).thenReturn(List.of(serviceEntity()));
        when(linkRepository.findActiveByServiceIds(List.of(1L))).thenReturn(List.of());
        when(bookingCatalogRepository.findSpecializationNamesByServiceIds(List.of(1L))).thenReturn(List.of());

        assertEquals(1, serviceService.getAllServices().size());
    }

    @Test
    void getServiceById() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(serviceEntity()));
        when(linkRepository.findActiveByServiceIds(List.of(1L))).thenReturn(List.of());
        when(bookingCatalogRepository.findSpecializationNamesByServiceIds(List.of(1L))).thenReturn(List.of());

        assertTrue(serviceService.getServiceById(1L).isPresent());
    }

    @Test
    void deleteService() {
        serviceService.deleteService(1L);

        verify(linkRepository).deleteByServiceId(1L);
        verify(serviceRepository).deleteById(1L);
    }

    @Test
    void setServiceSpecializations_serviceNotFound() {
        when(serviceRepository.existsById(99L)).thenReturn(false);

        assertTrue(serviceService.setServiceSpecializations(99L, List.of(1L)).isEmpty());
    }

    @Test
    void setServiceSpecializations_invalidSpec() {
        when(serviceRepository.existsById(1L)).thenReturn(true);
        when(specializationRepository.countByIdIn(List.of(99L))).thenReturn(0L);

        assertThrows(ResponseStatusException.class,
                () -> serviceService.setServiceSpecializations(1L, List.of(99L)));
    }

    @Test
    void getServiceByCode() {
        when(serviceRepository.findByCode("consult")).thenReturn(Optional.of(serviceEntity()));
        when(linkRepository.findActiveByServiceIds(List.of(1L))).thenReturn(List.of());
        when(bookingCatalogRepository.findSpecializationNamesByServiceIds(List.of(1L))).thenReturn(List.of());

        assertTrue(serviceService.getServiceByCode("consult").isPresent());
    }

    @Test
    void saveService() {
        pin122.kursovaya.model.Service entity = serviceEntity();
        when(serviceRepository.save(entity)).thenReturn(entity);
        when(linkRepository.findActiveByServiceIds(List.of(1L))).thenReturn(List.of());
        when(bookingCatalogRepository.findSpecializationNamesByServiceIds(List.of(1L))).thenReturn(List.of());

        ServiceDto saved = serviceService.saveService(entity);

        assertEquals("Consultation", saved.getName());
    }

    @Test
    void getServicesForDoctor_withData() {
        when(bookingCatalogRepository.findServiceIdsByDoctorId(1L)).thenReturn(List.of(1L));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(serviceEntity()));
        when(linkRepository.findActiveByServiceIds(List.of(1L))).thenReturn(List.of());
        when(bookingCatalogRepository.findSpecializationNamesByServiceIds(List.of(1L))).thenReturn(List.of());

        assertEquals(1, serviceService.getServicesForDoctor(1L).size());
    }

    @Test
    void setServiceSpecializations_success() {
        when(serviceRepository.existsById(1L)).thenReturn(true);
        when(specializationRepository.countByIdIn(List.of(2L))).thenReturn(1L);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(serviceEntity()));
        when(linkRepository.findActiveByServiceIds(List.of(1L))).thenReturn(List.of());
        when(bookingCatalogRepository.findSpecializationNamesByServiceIds(List.of(1L))).thenReturn(List.of());

        Optional<ServiceDto> result = serviceService.setServiceSpecializations(1L, List.of(2L));

        assertTrue(result.isPresent());
        verify(linkRepository).deleteByServiceId(1L);
        verify(linkRepository).save(any(SpecializationServiceLink.class));
    }
}
