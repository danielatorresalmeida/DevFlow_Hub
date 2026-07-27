package com.devflowhub.backend.controller;

import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.service.CollaboratorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collaborators")
public class CollaboratorController {

    private final CollaboratorService collaboratorService;

    public CollaboratorController(CollaboratorService collaboratorService) {
        this.collaboratorService = collaboratorService;
    }

    @GetMapping
    public List<Collaborator> findAll() {
        return collaboratorService.findAll();
    }

    @GetMapping("/{id}")
    public Collaborator findById(@PathVariable Long id) {
        return collaboratorService.getRequired(id);
    }

    @PostMapping
    public ResponseEntity<Collaborator> create(@Valid @RequestBody Collaborator collaborator) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collaboratorService.create(collaborator));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Collaborator> update(
            @PathVariable Long id,
            @Valid @RequestBody Collaborator collaborator
    ) {
        return ResponseEntity.ok(collaboratorService.update(id, collaborator));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        collaboratorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
