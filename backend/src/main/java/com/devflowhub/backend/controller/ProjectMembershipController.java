package com.devflowhub.backend.controller;

import com.devflowhub.backend.dto.AddProjectMemberRequest;
import com.devflowhub.backend.dto.ProjectMemberResponse;
import com.devflowhub.backend.dto.UpdateProjectMemberRequest;
import com.devflowhub.backend.service.ProjectMembershipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMembershipController {

    private final ProjectMembershipService projectMembershipService;

    public ProjectMembershipController(
            ProjectMembershipService projectMembershipService
    ) {
        this.projectMembershipService = projectMembershipService;
    }

    @GetMapping
    public List<ProjectMemberResponse> findAll(
            @PathVariable Long projectId
    ) {
        return projectMembershipService.findAll(projectId);
    }

    @PostMapping
    public ResponseEntity<ProjectMemberResponse> add(
            @PathVariable Long projectId,
            @Valid @RequestBody AddProjectMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectMembershipService.add(
                        projectId,
                        request
                ));
    }

    @PatchMapping("/{collaboratorId}")
    public ProjectMemberResponse updateRole(
            @PathVariable Long projectId,
            @PathVariable Long collaboratorId,
            @Valid @RequestBody UpdateProjectMemberRequest request
    ) {
        return projectMembershipService.updateRole(
                projectId,
                collaboratorId,
                request
        );
    }

    @DeleteMapping("/{collaboratorId}")
    public ResponseEntity<Void> remove(
            @PathVariable Long projectId,
            @PathVariable Long collaboratorId
    ) {
        projectMembershipService.remove(
                projectId,
                collaboratorId
        );
        return ResponseEntity.noContent().build();
    }
}
