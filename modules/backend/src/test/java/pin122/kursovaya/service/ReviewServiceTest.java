package pin122.kursovaya.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pin122.kursovaya.dto.ReviewDto;
import pin122.kursovaya.model.*;
import pin122.kursovaya.repository.ReviewRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @InjectMocks private ReviewService reviewService;

    private Review review;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setFirstName("Ivan");
        user.setLastName("Ivanov");
        user.setMiddleName("I.");
        Patient patient = new Patient();
        patient.setId(5L);
        patient.setUser(user);
        Doctor doctor = new Doctor();
        doctor.setId(3L);
        Appointment appointment = new Appointment();
        appointment.setId(7L);
        review = new Review();
        review.setId(1L);
        review.setPatient(patient);
        review.setDoctor(doctor);
        review.setAppointment(appointment);
        review.setRating((short) 5);
        review.setReviewText("Great");
    }

    @Test
    void getReviewsByDoctor() {
        when(reviewRepository.findByDoctorId(3L)).thenReturn(List.of(review));

        List<ReviewDto> list = reviewService.getReviewsByDoctor(3L);

        assertEquals(1, list.size());
        assertEquals("Ivanov Ivan I.", list.get(0).getPatientName());
    }

    @Test
    void getReviewById() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertTrue(reviewService.getReviewById(1L).isPresent());
    }

    @Test
    void getReviewByAppointmentId() {
        when(reviewRepository.findByAppointmentId(7L)).thenReturn(Optional.of(review));

        assertEquals((short) 5, reviewService.getReviewByAppointmentId(7L).get().getRating());
    }

    @Test
    void saveReview() {
        when(reviewRepository.save(review)).thenReturn(review);

        ReviewDto dto = reviewService.saveReview(review);

        assertEquals("Great", dto.getReviewText());
    }

    @Test
    void updateReview() {
        Review update = new Review();
        update.setRating((short) 4);
        update.setReviewText("Updated");
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));

        Optional<ReviewDto> dto = reviewService.updateReview(1L, update);

        assertTrue(dto.isPresent());
        assertEquals((short) 4, dto.get().getRating());
    }
}
