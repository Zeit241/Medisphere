package pin122.kursovaya.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import pin122.kursovaya.model.Patient;
import pin122.kursovaya.model.Role;
import pin122.kursovaya.model.User;
import pin122.kursovaya.repository.PatientRepository;
import pin122.kursovaya.repository.RoleRepository;
import pin122.kursovaya.repository.UserRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryConsistencyIT extends AbstractIntegrationIT {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PatientRepository patientRepository;

    @Test
    @Transactional
    void userRole_persistAndLoad() {
        Role role = ensureRole(roleRepository, "patient", "Patient");
        User user = createUser(userRepository, roleRepository, "repo1@test.com", "secret", "patient");

        Optional<User> loaded = userRepository.findById(user.getId());

        assertTrue(loaded.isPresent());
        assertEquals(role.getId(), loaded.get().getRole().getId());
    }

    @Test
    @Transactional
    void patient_userOneToOne() {
        User user = createUser(userRepository, roleRepository, "repo2@test.com", "secret", "patient");
        Patient patient = new Patient();
        patient.setUser(user);
        patient.setBirthDate(LocalDate.of(1995, 5, 5));
        patient.setGender((short) 1);
        patient.setCreatedAt(OffsetDateTime.now());
        patient.setUpdatedAt(OffsetDateTime.now());
        Patient saved = patientRepository.save(patient);

        assertEquals(user.getId(), saved.getUser().getId());
        assertTrue(patientRepository.findByUserId(user.getId()).isPresent());
    }

    @Test
    @Transactional
    void user_emailUniqueConstraint() {
        createUser(userRepository, roleRepository, "dup@test.com", "secret", "patient");
        User duplicate = new User();
        duplicate.setEmail(userRepository.findAll().get(0).getEmail());
        duplicate.setPasswordHash("hash");
        duplicate.setCreatedAt(OffsetDateTime.now());
        duplicate.setUpdatedAt(OffsetDateTime.now());

        assertThrows(Exception.class, () -> userRepository.saveAndFlush(duplicate));
    }

    @Test
    @Transactional
    void role_codeLookup() {
        Role created = new Role();
        created.setCode("doctor_lookup");
        created.setName("Doctor");
        roleRepository.save(created);

        assertTrue(roleRepository.findByCode("doctor_lookup").isPresent());
        assertEquals("Doctor", roleRepository.findByCode("doctor_lookup").get().getName());
    }

    @Test
    @Transactional
    void patient_deleteCascadesFromUserSide() {
        User user = createUser(userRepository, roleRepository, "repo3@test.com", "secret", "patient");
        Patient patient = new Patient();
        patient.setUser(user);
        patient.setCreatedAt(OffsetDateTime.now());
        patient.setUpdatedAt(OffsetDateTime.now());
        patientRepository.save(patient);

        Long patientId = patient.getId();
        patientRepository.deleteById(patientId);

        assertTrue(patientRepository.findById(patientId).isEmpty());
    }
}
