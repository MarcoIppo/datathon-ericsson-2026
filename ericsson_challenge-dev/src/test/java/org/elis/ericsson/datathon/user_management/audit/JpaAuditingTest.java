package org.elis.ericsson.datathon.user_management.audit;

import org.elis.ericsson.datathon.user_management.configuration.JpaAuditingConfig;
import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di regressione per UC-B-001: verifica che @EnableJpaAuditing
 * sia attivo e che i campi createdAt/updatedAt vengano popolati automaticamente.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class JpaAuditingTest {

    @Autowired
    private RoleRepository roleRepository;

    /**
     * Verifica che createdAt venga popolato automaticamente al persist di un'entità.
     */
    @Test
    void whenEntityPersisted_thenCreatedAtIsPopulated() {
        Role role = new Role("ROLE_TEST");
        Role saved = roleRepository.saveAndFlush(role);

        assertNotNull(saved.getCreatedAt(), "createdAt deve essere popolato automaticamente");
    }

    /**
     * Verifica che updatedAt venga aggiornato dopo una modifica all'entità.
     */
    @Test
    void whenEntityUpdated_thenUpdatedAtIsPopulated() throws InterruptedException {
        Role role = new Role("ROLE_TEST");
        Role saved = roleRepository.saveAndFlush(role);

        // piccola pausa per garantire timestamp diverso
        Thread.sleep(50);

        saved.setName("ROLE_UPDATED");
        Role updated = roleRepository.saveAndFlush(saved);

        assertNotNull(updated.getUpdatedAt(), "updatedAt deve essere popolato dopo update");
        assertTrue(updated.getUpdatedAt().compareTo(updated.getCreatedAt()) >= 0,
                "updatedAt deve essere >= createdAt");
    }
}
