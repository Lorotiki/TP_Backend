package com.tpi.history.exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        log.warn("History - ResponseStatusException: {} - {}", e.getStatusCode(), e.getReason());

        return ResponseEntity
                .status(e.getStatusCode())
                .body(new ErrorResponse(
                        e.getStatusCode().value(),
                        e.getReason() != null ? e.getReason() : "Error en la solicitud",
                        OffsetDateTime.now()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("History - Excepción inesperada", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        500,
                        "Error interno del servidor",
                        OffsetDateTime.now()
                ));
    }
}

record ErrorResponse(
        int code,
        String message,
        OffsetDateTime timestamp
) {}