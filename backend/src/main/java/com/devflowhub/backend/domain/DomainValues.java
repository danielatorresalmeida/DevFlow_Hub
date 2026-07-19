package com.devflowhub.backend.domain;

import com.devflowhub.backend.exception.InvalidOperationException;

import java.util.Set;

public final class DomainValues {

    public static final class ProjectStatus {
        public static final String PLANNED = "PLANNED";
        public static final String IN_PROGRESS = "IN_PROGRESS";
        public static final String COMPLETED = "COMPLETED";
        public static final Set<String> ALL = Set.of(PLANNED, IN_PROGRESS, COMPLETED);

        private ProjectStatus() {
        }
    }

    public static final class TaskStatus {
        public static final String PENDING = "PENDING";
        public static final String IN_PROGRESS = "IN_PROGRESS";
        public static final String REVIEW = "REVIEW";
        public static final String COMPLETED = "COMPLETED";
        public static final Set<String> ALL = Set.of(PENDING, IN_PROGRESS, REVIEW, COMPLETED);

        private TaskStatus() {
        }
    }

    public static final class TaskPriority {
        public static final String LOW = "LOW";
        public static final String MEDIUM = "MEDIUM";
        public static final String HIGH = "HIGH";
        public static final Set<String> ALL = Set.of(LOW, MEDIUM, HIGH);

        private TaskPriority() {
        }
    }

    public static final class ProgramStatus {
        public static final String PLANNED = "PLANNED";
        public static final String ACTIVE = "ACTIVE";
        public static final String COMPLETED = "COMPLETED";
        public static final Set<String> ALL = Set.of(PLANNED, ACTIVE, COMPLETED);

        private ProgramStatus() {
        }
    }

    private DomainValues() {
    }

    public static void requireAllowed(String value, Set<String> allowedValues, String fieldName) {
        if (!allowedValues.contains(value)) {
            throw new InvalidOperationException(fieldName + " has an unsupported value.");
        }
    }
}
