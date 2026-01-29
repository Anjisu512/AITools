package com.aitool.aitool.config;

import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApiExceptionBuild extends RuntimeException {

    private final String strStatus;
    private final HttpStatus status;
    
    // httpcode에 따른 exception반환
    public ApiExceptionBuild(HttpStatus status, String message) {
            super(message);
            this.strStatus = String.valueOf(status.value());
            this.status = status;
        }

    public String getStrStatus() {
        return strStatus;
    }
    
    public HttpStatus getStatus() {
        return status;
    }

}
