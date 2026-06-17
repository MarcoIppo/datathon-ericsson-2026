package org.elis.ericsson.datathon.user_management.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elis.ericsson.datathon.user_management.model.dto.LoginDto;
import org.elis.ericsson.datathon.user_management.model.dto.request.SignUpRequestDto;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.entity.eggup.EggUpInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for password exposure vulnerabilities (UC-S-002).
 * No Spring context — plain JUnit 5 + ObjectMapper.
 */
class PasswordExposureTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Scenario: LoginDto.toString() viene invocato da un logger SLF4J.
     * Precondizioni: LoginDto popolato con email e password reale.
     * Risultato atteso: toString() contiene "[PROTECTED]" e NON la password in chiaro.
     */
    @Test
    void shouldMaskPasswordInLoginDtoToString() {
        LoginDto dto = new LoginDto("user@test.com", "s3cr3tP@ss");
        String result = dto.toString();
        assertFalse(result.contains("s3cr3tP@ss"), "La password non deve apparire in toString()");
        assertTrue(result.contains("[PROTECTED]"), "toString() deve contenere [PROTECTED]");
    }

    /**
     * Scenario: SignUpRequestDto.toString() viene invocato da un logger SLF4J durante la registrazione.
     * Precondizioni: SignUpRequestDto popolato con tutti i campi inclusa la password.
     * Risultato atteso: toString() contiene "[PROTECTED]" e NON la password in chiaro.
     */
    @Test
    void shouldMaskPasswordInSignUpRequestDtoToString() {
        SignUpRequestDto dto = new SignUpRequestDto();
        dto.setFirstName("Mario");
        dto.setLastName("Rossi");
        dto.setEmail("mario@test.com");
        dto.setPassword("my$ecretPwd99");
        String result = dto.toString();
        assertFalse(result.contains("my$ecretPwd99"), "La password non deve apparire in toString()");
        assertTrue(result.contains("[PROTECTED]"), "toString() deve contenere [PROTECTED]");
    }

    /**
     * Scenario: GET /api/profiles serializza List<UserProfile> con Jackson.
     * Precondizioni: UserProfile con password bcrypt hash impostata.
     * Risultato atteso: il JSON serializzato NON contiene il campo "password".
     */
    @Test
    void shouldNotSerializePasswordInUserProfileJson() throws Exception {
        UserProfile profile = new UserProfile();
        profile.setEmail("test@test.com");
        profile.setPassword("$2a$10$hashedPasswordValue");
        String json = mapper.writeValueAsString(profile);
        assertFalse(json.contains("\"password\""), "Il campo password non deve essere serializzato nel JSON");
        assertFalse(json.contains("hashedPasswordValue"), "L'hash della password non deve apparire nel JSON");
    }

    /**
     * Scenario: GET /api/profiles serializza EggUpInfo tramite relazione OneToOne in UserProfile.
     * Precondizioni: EggUpInfo con password del servizio EggUp impostata.
     * Risultato atteso: il JSON serializzato NON contiene il campo "password".
     */
    @Test
    void shouldNotSerializePasswordInEggUpInfoJson() throws Exception {
        EggUpInfo info = new EggUpInfo();
        info.setUsername("eggup_user_42");
        info.setPassword("eggUpS3rviceP@ss");
        String json = mapper.writeValueAsString(info);
        assertFalse(json.contains("\"password\""), "Il campo password non deve essere serializzato nel JSON");
        assertFalse(json.contains("eggUpS3rviceP@ss"), "La password EggUp non deve apparire nel JSON");
    }

    /**
     * Scenario: deserializzazione JSON → LoginDto (write path deve funzionare).
     * Precondizioni: JSON valido con campo "password".
     * Risultato atteso: il campo password viene popolato correttamente nel DTO.
     */
    @Test
    void shouldDeserializePasswordInLoginDto() throws Exception {
        String json = "{\"email\":\"user@test.com\",\"password\":\"myPassword\"}";
        LoginDto dto = mapper.readValue(json, LoginDto.class);
        assertEquals("myPassword", dto.getPassword(), "La deserializzazione deve popolare il campo password");
        assertEquals("user@test.com", dto.getEmail());
    }
}
