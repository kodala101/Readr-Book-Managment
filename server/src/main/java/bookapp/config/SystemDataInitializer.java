package bookapp.config;

import bookapp.entities.User;
import bookapp.enums.Role;
import bookapp.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * Component responsible for seeding essential system data into the database upon application startup.
 * <p>
 * Implements {@link CommandLineRunner} to execute initialization tasks, ensuring default system accounts
 * (such as a fallback system/ghost account and an initial administrative user) exist in PostgreSQL.
 */
@Component
@RequiredArgsConstructor
public class SystemDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-password:book_admin_pass}")
    private String adminDefaultPassword;

    /**
     * Executes data initialization logic automatically when the Spring application context finishes starting up.
     *
     * @param args incoming command-line arguments passed to the application
     * @throws Exception if an error occurs during database operations
     */
    @Override
    @Transactional
    public void run(String @NonNull ... args) throws Exception {
        initGhostUser();
        initAdminUser();
    }

    /**
     * Seeds a protected system account ("ghostUser") into the database if it does not already exist.
     * <p>
     * This ghost user acts as a system placeholder for reassigning ownership of orphaned records
     * (e.g., reviews or journal entries) when regular user accounts are deleted. Assigns a random,
     * unguessable password string so nobody can log into this account directly.
     */
    private void initGhostUser() {
        if (userRepository.findByUsername("ghostUser").isEmpty()) {
            User ghostUser = new User();
            ghostUser.setUsername("ghostUser");
            ghostUser.setEmail("ghost@bookapp.com");
            ghostUser.setPassword("PROTECTED_SYSTEM_ACCOUNT_" + UUID.randomUUID());
            ghostUser.setRole(Role.USER);

            userRepository.save(ghostUser);
        }
    }

    /**
     * Seeds the initial system administrator account ("book_admin") into the database if neither
     * the admin username nor admin email address is already present.
     * <p>
     * Securely hashes the default administrator password using the configured {@link PasswordEncoder}.
     */
    private void initAdminUser() {
        if (userRepository.findByUsername("book_admin").isEmpty() && userRepository.findByEmail("admin@bookapp.com").isEmpty()) {
            User admin = new User();
            admin.setUsername("book_admin");
            admin.setEmail("admin@bookapp.com");
            admin.setPassword(passwordEncoder.encode(adminDefaultPassword));
            admin.setRole(Role.ADMIN);

            userRepository.save(admin);
        }
    }
}
