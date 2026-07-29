package com.devflowhub.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler exceptionHandler =
            new ApiExceptionHandler();

    @Test
    void projectAccessDeniedReturnsForbiddenApiError() {
        ResponseEntity<ApiError> response = exceptionHandler
                .handleProjectAccessDenied(
                        new ProjectAccessDeniedException()
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo(
                "You do not have permission to perform this operation."
        );
        assertThat(response.getBody().validationErrors()).isEmpty();
        assertThat(response.getBody().timestamp()).isNotNull();
    }
    @Test
    void resourceConflictReturnsConflictApiError() {
        ResponseEntity<ApiError> response = exceptionHandler
                .handleResourceConflict(
                        new ResourceConflictException(
                                "Membership conflict."
                        )
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message())
                .isEqualTo("Membership conflict.");
        assertThat(response.getBody().validationErrors()).isEmpty();
    }

    @Test
    void optimisticLockingFailureReturnsGenericConflictMessage() {
        ResponseEntity<ApiError> response = exceptionHandler
                .handleOptimisticLockingFailure(
                        new ObjectOptimisticLockingFailureException(
                                "ProjectMembership",
                                12L
                        )
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo(
                "The resource was modified by another request. Refresh and try again."
        );
        assertThat(response.getBody().validationErrors()).isEmpty();
    }

}
