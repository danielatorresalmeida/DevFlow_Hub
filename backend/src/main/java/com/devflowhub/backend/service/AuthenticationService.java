package com.devflowhub.backend.service;

import com.devflowhub.backend.dto.AuthenticatedCollaboratorResponse;
import com.devflowhub.backend.dto.ChangePasswordRequest;
import com.devflowhub.backend.dto.LoginRequest;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.exception.AuthenticationFailedException;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthenticationService {

    private final CollaboratorRepository collaboratorRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(
            CollaboratorRepository collaboratorRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.collaboratorRepository = collaboratorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthenticatedCollaboratorResponse login(LoginRequest request) {
        return toResponse(authenticateRequired(request.email(), request.password()));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Collaborator collaborator = authenticateRequired(
                request.email(),
                request.currentPassword()
        );

        if (passwordMatches(request.newPassword(), collaborator.getPassword())) {
            throw new InvalidOperationException(
                    "New password must be different from the current password."
            );
        }

        collaborator.setPassword(passwordEncoder.encode(request.newPassword()));
        collaboratorRepository.save(collaborator);
    }

    private Collaborator authenticateRequired(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationFailedException();
        }

        return collaboratorRepository.findByEmailIgnoreCase(email.trim())
                .filter(collaborator -> Boolean.TRUE.equals(collaborator.getActive()))
                .filter(collaborator ->
                        passwordMatches(password, collaborator.getPassword())
                )
                .orElseThrow(AuthenticationFailedException::new);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        try {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private AuthenticatedCollaboratorResponse toResponse(Collaborator collaborator) {
        return new AuthenticatedCollaboratorResponse(
                collaborator.getId(),
                collaborator.getName(),
                collaborator.getEmail(),
                collaborator.getRole(),
                Boolean.TRUE.equals(collaborator.getActive())
        );
    }
}
