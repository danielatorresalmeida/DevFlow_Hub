package com.devflowhub.backend.controller;

import com.devflowhub.backend.dto.ProjectOwnershipTransferResponse;
import com.devflowhub.backend.dto.TransferProjectOwnershipRequest;
import com.devflowhub.backend.service.ProjectOwnershipService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/ownership-transfer")
public class ProjectOwnershipController {

    private final ProjectOwnershipService projectOwnershipService;

    public ProjectOwnershipController(
            ProjectOwnershipService projectOwnershipService
    ) {
        this.projectOwnershipService = projectOwnershipService;
    }

    @PostMapping
    public ProjectOwnershipTransferResponse transfer(
            @PathVariable Long projectId,
            @Valid @RequestBody TransferProjectOwnershipRequest request
    ) {
        return projectOwnershipService.transfer(projectId, request);
    }
}
