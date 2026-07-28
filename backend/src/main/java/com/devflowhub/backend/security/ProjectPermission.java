package com.devflowhub.backend.security;

import com.devflowhub.backend.domain.ProjectMembershipRole;

import java.util.EnumSet;
import java.util.Set;

public enum ProjectPermission {

    VIEW_PROJECT(
            EnumSet.allOf(ProjectMembershipRole.class)
    ),

    CONTRIBUTE_TO_PROJECT(
            EnumSet.of(
                    ProjectMembershipRole.OWNER,
                    ProjectMembershipRole.MANAGER,
                    ProjectMembershipRole.CONTRIBUTOR
            )
    ),

    MANAGE_PROJECT(
            EnumSet.of(
                    ProjectMembershipRole.OWNER,
                    ProjectMembershipRole.MANAGER
            )
    ),

    DELETE_PROJECT(
            EnumSet.of(ProjectMembershipRole.OWNER)
    );

    private final Set<ProjectMembershipRole> allowedRoles;

    ProjectPermission(Set<ProjectMembershipRole> allowedRoles) {
        this.allowedRoles = Set.copyOf(allowedRoles);
    }

    public boolean isGrantedTo(ProjectMembershipRole role) {
        return role != null && allowedRoles.contains(role);
    }
}
