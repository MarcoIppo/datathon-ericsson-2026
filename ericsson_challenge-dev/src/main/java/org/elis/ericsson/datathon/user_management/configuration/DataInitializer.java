package org.elis.ericsson.datathon.user_management.configuration;

import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.repository.RoleRepository;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserProfileRepository userProfileRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userProfileRepository = userProfileRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userProfileRepository.count() > 0) {
            logger.info("Default admin already exists, skipping initialization");
            return;
        }

        try {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(createRole("ROLE_ADMIN")));
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(createRole("ROLE_USER")));

            UserProfile admin = new UserProfile();
            admin.setEmail("admin@elis.org");
            admin.setFirstName("firstName_admin");
            admin.setLastName("lastName_admin");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setRoles(new ArrayList<>(List.of(adminRole, userRole)));

            userProfileRepository.save(admin);
            logger.info("Default admin user created successfully");
        } catch (Exception e) {
            logger.warn("Could not create default admin user: {}", e.getMessage());
        }
    }

    private Role createRole(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
