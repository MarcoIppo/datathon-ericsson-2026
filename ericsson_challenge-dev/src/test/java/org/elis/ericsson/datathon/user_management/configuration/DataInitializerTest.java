package org.elis.ericsson.datathon.user_management.configuration;

import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.repository.RoleRepository;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(userProfileRepository, roleRepository, passwordEncoder);
    }

    /**
     * Scenario: Database vuoto al primo avvio.
     * Precondizioni: userProfileRepository.count() == 0, ruoli già presenti nel DB.
     * Risultato atteso: viene creato un UserProfile con email "admin@elis.org",
     * password encodata e 2 ruoli (ROLE_ADMIN, ROLE_USER).
     */
    @Test
    void shouldCreateAdminUser_whenDatabaseIsEmpty() {
        when(userProfileRepository.count()).thenReturn(0L);
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");

        dataInitializer.run();

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        UserProfile saved = captor.getValue();
        assertEquals("admin@elis.org", saved.getEmail());
        assertEquals("encoded_password", saved.getPassword());
        assertEquals(2, saved.getRoles().size());
    }

    /**
     * Scenario: Database già popolato con utenti esistenti.
     * Precondizioni: userProfileRepository.count() > 0.
     * Risultato atteso: nessuna interazione con roleRepository né salvataggio di nuovi utenti.
     */
    @Test
    void shouldSkipInitialization_whenUsersAlreadyExist() {
        when(userProfileRepository.count()).thenReturn(5L);

        dataInitializer.run();

        verify(userProfileRepository, never()).save(any(UserProfile.class));
        verify(roleRepository, never()).findByName(anyString());
    }

    /**
     * Scenario: Database vuoto e ruoli non ancora presenti.
     * Precondizioni: userProfileRepository.count() == 0, roleRepository.findByName() restituisce empty.
     * Risultato atteso: vengono creati e salvati entrambi i ruoli (ROLE_ADMIN, ROLE_USER)
     * prima di creare l'utente admin.
     */
    @Test
    void shouldCreateRoles_whenRolesDoNotExist() {
        when(userProfileRepository.count()).thenReturn(0L);
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");

        dataInitializer.run();

        verify(roleRepository, times(2)).save(any(Role.class));
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    /**
     * Scenario: Eccezione durante il salvataggio dell'utente (es. constraint violation, race condition).
     * Precondizioni: userProfileRepository.save() lancia RuntimeException.
     * Risultato atteso: l'eccezione viene catturata internamente, l'applicazione non crasha.
     */
    @Test
    void shouldNotCrash_whenSaveThrowsException() {
        when(userProfileRepository.count()).thenReturn(0L);
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(new Role()));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(new Role()));
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");
        when(userProfileRepository.save(any(UserProfile.class))).thenThrow(new RuntimeException("Constraint violation"));

        assertDoesNotThrow(() -> dataInitializer.run());
    }
}
