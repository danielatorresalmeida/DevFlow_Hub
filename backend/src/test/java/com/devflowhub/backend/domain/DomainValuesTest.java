package com.devflowhub.backend.domain;

import com.devflowhub.backend.exception.InvalidOperationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainValuesTest {

    @Test
    void acceptsSupportedTaskStatus() {
        assertThatCode(() -> DomainValues.requireAllowed(
                DomainValues.TaskStatus.REVIEW,
                DomainValues.TaskStatus.ALL,
                "Task status"
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsupportedTaskStatus() {
        assertThatThrownBy(() -> DomainValues.requireAllowed(
                "UNKNOWN",
                DomainValues.TaskStatus.ALL,
                "Task status"
        ))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Task status has an unsupported value.");
    }
}
