package org.elis.ericsson.datathon.user_management.security;

import org.elis.ericsson.datathon.user_management.constants.SecurityConstants;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-S-001: Verifica che il JWT secret non sia più hardcoded in SecurityConstants
 * e che il secret sia iniettato esternamente.
 */
class JwtSecretExternalizationTest {

    /**
     * Verifica che SecurityConstants non contenga più un campo JWT_SECRET.
     */
    @Test
    void securityConstants_shouldNotExposeJwtSecret() {
        boolean hasJwtSecret = Arrays.stream(SecurityConstants.class.getDeclaredFields())
                .anyMatch(f -> f.getName().equals("JWT_SECRET"));

        assertFalse(hasJwtSecret, "SecurityConstants non deve più contenere il campo JWT_SECRET");
    }

    /**
     * Verifica che JwtUtility abbia un campo jwtSecret (iniettato, non hardcoded).
     */
    @Test
    void jwtUtility_shouldHaveInjectableSecretField() throws NoSuchFieldException {
        Field field = JwtUtility.class.getDeclaredField("jwtSecret");
        assertNotNull(field, "JwtUtility deve avere un campo 'jwtSecret' per l'injection");
        assertTrue(field.isAnnotationPresent(org.springframework.beans.factory.annotation.Value.class),
                "Il campo jwtSecret deve essere annotato con @Value");
    }
}
