package com.ledgerline.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final int status;
    private final String error;
    private final String message;
    private final Instant timestamp;
    private final List<String> fieldErrors;   // populated only for validation failures
}
