package com.devflowhub.backend.service;

import com.devflowhub.backend.dto.AuthenticatedCollaboratorResponse;
import com.devflowhub.backend.dto.ChangePasswordRequest;
import com.devflowhub.backend.dto.LoginRequest;
import com.devflowhub.backend.dto.LoginResponse;
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
    private final JwtTokenService jwtTokenService;

    public AuthenticationService(
            CollaboratorRepository collaboratorRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.collaboratorRepository = collaboratorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResponse login(LoginRequest request) {
        Collaborator collaborator = authenticateRequired(
                request.email(),
                request.password()
        );

        JwtTokenService.IssuedToken issuedToken =
                jwtTokenService.issue(collaborator);

        return new LoginResponse(
                issuedToken.accessToken(),
                "Bearer",
                issuedToken.expiresInSeconds(),
                toResponse(collaborator)
        );
    }

    @Transactional
    public void changePassword(
            Long authenticatedCollaboratorId,
            ChangePasswordRequest request
    ) {
        Collaborator collaborator = collaboratorRepository
                .findById(authenticatedCollaboratorId)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new AuthenticationFailedException(
                        "The authenticated collaborator is not active."
                ));

        if (!passwordMatches(
                request.currentPassword(),
                collaborator.getPassword()
        )) {
            throw new AuthenticationFailedException(
                    "Current password is invalid."
            );
        }

        if (passwordMatches(
                request.newPassword(),
                collaborator.getPassword()
        )) {
            throw new InvalidOperationException(
                    "New password must be different from the current password."
            );
        }

        collaborator.setPassword(
                passwordEncoder.encode(request.newPassword())
        );

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
                collaborator.getSystemRole(),
                Boolean.TRUE.equals(collaborator.getActive())
        );
    }
}
