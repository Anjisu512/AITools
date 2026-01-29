package com.aitool.aitool.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiExceptionBuild.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiExceptionBuild e) {

        return ResponseEntity.status(e.getStatus()).body(new ApiErrorResponse(e.getMessage()));
    }
}
