package com.ucc.aiproxy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HTTP body for POST /api/ai/generate. */
public record GenerateRequestBody(
        @NotBlank(message = "Prompt must not be empty")
        @Size(max = 4000, message = "Prompt must have at most 4000 characters")
        String prompt) {
}
