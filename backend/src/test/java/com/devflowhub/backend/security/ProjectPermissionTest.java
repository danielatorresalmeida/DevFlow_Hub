package com.devflowhub.backend.security;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectPermissionTest {

    @Test
    void ownerReceivesEveryProjectPermission() {
        assertThat(grantedPermissions(ProjectMembershipRole.OWNER))
                .containsExactly(
                        ProjectPermission.VIEW_PROJECT,
                        ProjectPermission.CONTRIBUTE_TO_PROJECT,
                        ProjectPermission.MANAGE_PROJECT,
                        ProjectPermission.MANAGE_PROJECT_MEMBERS,
                        ProjectPermission.TRANSFER_PROJECT_OWNERSHIP,
                        ProjectPermission.DELETE_PROJECT
                );
    }

    @Test
    void managerCannotDeleteProject() {
        assertThat(grantedPermissions(ProjectMembershipRole.MANAGER))
                .containsExactly(
                        ProjectPermission.VIEW_PROJECT,
                        ProjectPermission.CONTRIBUTE_TO_PROJECT,
                        ProjectPermission.MANAGE_PROJECT,
                        ProjectPermission.MANAGE_PROJECT_MEMBERS
                );
    }

    @Test
    void contributorCanViewAndContributeOnly() {
        assertThat(grantedPermissions(ProjectMembershipRole.CONTRIBUTOR))
                .containsExactly(
                        ProjectPermission.VIEW_PROJECT,
                        ProjectPermission.CONTRIBUTE_TO_PROJECT
                );
    }

    @Test
    void viewerCanOnlyViewProject() {
        assertThat(grantedPermissions(ProjectMembershipRole.VIEWER))
                .containsExactly(ProjectPermission.VIEW_PROJECT);
    }

    @Test
    void nullRoleReceivesNoPermission() {
        assertThat(grantedPermissions(null)).isEmpty();
    }

    private List<ProjectPermission> grantedPermissions(
            ProjectMembershipRole role
    ) {
        return Arrays.stream(ProjectPermission.values())
                .filter(permission -> permission.isGrantedTo(role))
                .toList();
    }
}
