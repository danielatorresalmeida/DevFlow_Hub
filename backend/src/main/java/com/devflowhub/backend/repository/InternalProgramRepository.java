package com.devflowhub.backend.repository;

import com.devflowhub.backend.entity.InternalProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InternalProgramRepository extends JpaRepository<InternalProgram, Long> {
}
