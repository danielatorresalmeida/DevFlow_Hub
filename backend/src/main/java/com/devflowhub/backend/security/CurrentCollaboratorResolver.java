package com.devflowhub.backend.security;

import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.exception.AuthenticationFailedException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentCollaboratorResolver {

    private static final String AUTHENTICATION_REQUIRED_MESSAGE =
            "Authentication is required.";

    private static final String INVALID_AUTHENTICATION_MESSAGE =
            "Authentication is invalid.";

    private static final String COLLABORATOR_UNAVAILABLE_MESSAGE =
            "The authenticated collaborator is not active.";

    private final CollaboratorRepository collaboratorRepository;

    public CurrentCollaboratorResolver(
            CollaboratorRepository collaboratorRepository
    ) {
        this.collaboratorRepository = collaboratorRepository;
    }

    public Collaborator getRequired() {
        Long collaboratorId = getRequiredId();

        return collaboratorRepository
                .findById(collaboratorId)
                .filter(collaborator ->
                        Boolean.TRUE.equals(collaborator.getActive())
                )
                .orElseThrow(() -> new AuthenticationFailedException(
                        COLLABORATOR_UNAVAILABLE_MESSAGE
                ));
    }

    public Long getRequiredId() {
        JwtAuthenticationToken jwtAuthentication =
                getRequiredJwtAuthentication();

        return parseCollaboratorId(
                jwtAuthentication.getToken().getSubject()
        );
    }

    private JwtAuthenticationToken getRequiredJwtAuthentication() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            throw new AuthenticationFailedException(
                    AUTHENTICATION_REQUIRED_MESSAGE
            );
        }

        return jwtAuthentication;
    }

    private Long parseCollaboratorId(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new AuthenticationFailedException(
                    INVALID_AUTHENTICATION_MESSAGE
            );
        }

        try {
            long collaboratorId = Long.parseLong(subject);

            if (collaboratorId <= 0) {
                throw new AuthenticationFailedException(
                        INVALID_AUTHENTICATION_MESSAGE
                );
            }

            return collaboratorId;
        }
        catch (NumberFormatException exception) {
            throw new AuthenticationFailedException(
                    INVALID_AUTHENTICATION_MESSAGE
            );
        }
    }
}
