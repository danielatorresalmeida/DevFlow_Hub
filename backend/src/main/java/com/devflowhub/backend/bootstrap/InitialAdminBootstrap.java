package com.devflowhub.backend.bootstrap;

import com.devflowhub.backend.config.AdminBootstrapProperties;
import com.devflowhub.backend.domain.SystemRole;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.util.TextNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(
        prefix = "app.bootstrap-admin",
        name = "enabled",
        havingValue = "true"
)
public class InitialAdminBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            InitialAdminBootstrap.class
    );

    private static final String ADMIN_ROLE = "System Administrator";
    private static final int MINIMUM_PASSWORD_LENGTH = 12;
    private static final int MAXIMUM_PASSWORD_LENGTH = 64;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );

    private final AdminBootstrapProperties properties;
    private final CollaboratorRepository collaboratorRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialAdminBootstrap(
            AdminBootstrapProperties properties,
            CollaboratorRepository collaboratorRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.collaboratorRepository = collaboratorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (!properties.enabled()) {
            return;
        }

        if (collaboratorRepository.existsBySystemRole(SystemRole.ADMIN)) {
            throw new IllegalStateException(
                    "An administrator already exists. Disable the initial "
                            + "administrator bootstrap."
            );
        }

        String name = TextNormalizer.trim(properties.name());
        String email = TextNormalizer.lower(properties.email());
        String password = properties.password();

        validateName(name);
        validateEmail(email);
        validatePassword(password);

        if (collaboratorRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException(
                    "The bootstrap administrator email is already in use."
            );
        }

        Collaborator administrator = new Collaborator();
        administrator.setName(name);
        administrator.setEmail(email);
        administrator.setPassword(passwordEncoder.encode(password));
        administrator.setRole(ADMIN_ROLE);
        administrator.setSystemRole(SystemRole.ADMIN);
        administrator.setActive(true);

        collaboratorRepository.save(administrator);

        LOGGER.info(
                "Initial system administrator created for {}. Disable the "
                        + "bootstrap before the next application start.",
                email
        );
    }

    private void validateName(String name) {
        if (name.isBlank()) {
            throw new IllegalStateException(
                    "DEVFLOW_BOOTSTRAP_ADMIN_NAME must not be blank."
            );
        }

        if (name.length() > 150) {
            throw new IllegalStateException(
                    "DEVFLOW_BOOTSTRAP_ADMIN_NAME must have at most 150 characters."
            );
        }
    }

    private void validateEmail(String email) {
        if (email.isBlank()) {
            throw new IllegalStateException(
                    "DEVFLOW_BOOTSTRAP_ADMIN_EMAIL must not be blank."
            );
        }

        if (email.length() > 150 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalStateException(
                    "DEVFLOW_BOOTSTRAP_ADMIN_EMAIL must be a valid email address."
            );
        }
    }

    private void validatePassword(String password) {
        if (password == null
                || password.length() < MINIMUM_PASSWORD_LENGTH
                || password.length() > MAXIMUM_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "DEVFLOW_BOOTSTRAP_ADMIN_PASSWORD must have between 12 and 64 characters."
            );
        }

        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(character ->
                !Character.isLetterOrDigit(character)
        );

        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
            throw new IllegalStateException(
                    "DEVFLOW_BOOTSTRAP_ADMIN_PASSWORD must include uppercase, "
                            + "lowercase, numeric and special characters."
            );
        }
    }
}
