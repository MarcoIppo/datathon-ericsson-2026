package org.elis.ericsson.datathon.user_management.model.entity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-B-002: Verifica che nessuna entity nel package model.entity
 * sia annotata con @EnableAutoConfiguration.
 */
class NoEnableAutoConfigOnEntityTest {

    @Test
    void noEntityShouldHaveEnableAutoConfiguration() {
        List<String> violations = new ArrayList<>();
        String packageName = "org.elis.ericsson.datathon.user_management.model.entity";
        String path = packageName.replace('.', '/');
        File dir = new File("src/main/java/" + path);

        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".java")) {
                    String className = packageName + "." + file.getName().replace(".java", "");
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(EnableAutoConfiguration.class)) {
                            violations.add(clazz.getSimpleName());
                        }
                    } catch (ClassNotFoundException ignored) {
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Le seguenti entity usano @EnableAutoConfiguration (vietato): " + violations);
    }
}
