package org.elis.ericsson.datathon.user_management.model.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * UC-S-005: Test del validator @NoXss.
 * Verifica che payload XSS vengano rifiutati e nomi internazionali accettati.
 */
class NoXssValidatorTest {

    private NoXssValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new NoXssValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    /** Payload XSS devono essere rifiutati */
    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "<img src=x onerror=\"alert(1)\">",
            "<svg onload=alert(1)>",
            "\"><script>alert(1)</script>",
            "&lt;script&gt;",
            "&#60;script&#62;",
            "test\\x3cscript"
    })
    void shouldRejectXssPayloads(String payload) {
        assertFalse(validator.isValid(payload, context), "Dovrebbe rifiutare: " + payload);
    }

    /** Nomi internazionali devono essere accettati */
    @ParameterizedTest
    @ValueSource(strings = {
            "Mario Rossi",
            "O'Brien",
            "Jean-Pierre",
            "François",
            "Müller",
            "D'Angelo",
            "田中太郎",
            "Дмитрий Петров",
            "María José",
            "+39 333 1234567",
            "(06) 123-4567",
            "user@example.com"
    })
    void shouldAcceptInternationalNames(String value) {
        assertTrue(validator.isValid(value, context), "Dovrebbe accettare: " + value);
    }

    /** Null deve essere accettato (gestito da @NotBlank separatamente) */
    @Test
    void shouldAcceptNull() {
        assertTrue(validator.isValid(null, context));
    }
}
