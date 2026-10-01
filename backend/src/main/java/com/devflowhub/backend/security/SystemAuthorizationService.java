package com.devflowhub.backend.security;

import com.devflowhub.backend.domain.SystemRole;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.exception.SystemAccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SystemAuthorizationService {

    private final CurrentCollaboratorResolver currentCollaboratorResolver;

    public SystemAuthorizationService(
            CurrentCollaboratorResolver currentCollaboratorResolver
    ) {
        this.currentCollaboratorResolver = currentCollaboratorResolver;
    }

    public Collaborator requireAdmin() {
        Collaborator collaborator = currentCollaboratorResolver.getRequired();

        if (collaborator.getSystemRole() != SystemRole.ADMIN) {
            throw new SystemAccessDeniedException();
        }

        return collaborator;
    }
}
