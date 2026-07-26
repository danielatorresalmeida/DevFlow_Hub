package com.devflowhub.backend.controller;

import com.devflowhub.backend.exception.ApiExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new UploadLimitTestController()
                )
                .setControllerAdvice(
                        new ApiExceptionHandler()
                )
                .build();
    }

    @Test
    void maxUploadSizeReturnsHttp413()
            throws Exception {
        mockMvc.perform(
                        post("/test/upload-limit")
                )
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.status")
                        .value(413))
                .andExpect(jsonPath("$.message")
                        .value(
                                "The uploaded file exceeds the configured size limit."
                        ));
    }

    @RestController
    private static class UploadLimitTestController {

        @PostMapping("/test/upload-limit")
        void rejectOversizedUpload() {
            throw new MaxUploadSizeExceededException(
                    10L * 1024L * 1024L
            );
        }
    }
}