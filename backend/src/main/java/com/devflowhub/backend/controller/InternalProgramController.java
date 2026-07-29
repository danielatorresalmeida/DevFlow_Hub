package com.devflowhub.backend.controller;

import com.devflowhub.backend.entity.InternalProgram;
import com.devflowhub.backend.service.InternalProgramService;
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
@RequestMapping("/api/internal-programs")
public class InternalProgramController {

    private final InternalProgramService internalProgramService;

    public InternalProgramController(InternalProgramService internalProgramService) {
        this.internalProgramService = internalProgramService;
    }

    @GetMapping
    public List<InternalProgram> findAll() {
        return internalProgramService.findAll();
    }

    @GetMapping("/{id}")
    public InternalProgram findById(@PathVariable Long id) {
        return internalProgramService.getRequired(id);
    }

    @PostMapping
    public ResponseEntity<InternalProgram> create(@Valid @RequestBody InternalProgram internalProgram) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(internalProgramService.create(internalProgram));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InternalProgram> update(
            @PathVariable Long id,
            @Valid @RequestBody InternalProgram internalProgram
    ) {
        return ResponseEntity.ok(internalProgramService.update(id, internalProgram));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        internalProgramService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
