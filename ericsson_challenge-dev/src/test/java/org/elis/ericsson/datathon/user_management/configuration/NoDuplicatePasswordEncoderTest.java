package org.elis.ericsson.datathon.user_management.configuration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-B-004: Verifica che nessuna classe (esclusa SecurityConfig) istanzi
 * direttamente new BCryptPasswordEncoder().
 */
class NoDuplicatePasswordEncoderTest {

    @Test
    void noBCryptPasswordEncoderInstantiationOutsideSecurityConfig() throws IOException {
        Path srcDir = Paths.get("src/main/java");
        try (Stream<Path> files = Files.walk(srcDir)) {
            List<String> violations = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals("SecurityConfig.java"))
                    .filter(p -> {
                        try {
                            return Files.readString(p).contains("new BCryptPasswordEncoder()");
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());

            assertTrue(violations.isEmpty(),
                    "Le seguenti classi istanziano BCryptPasswordEncoder direttamente (usare injection): " + violations);
        }
    }
}
