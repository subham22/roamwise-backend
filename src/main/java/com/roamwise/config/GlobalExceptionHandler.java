package com.roamwise.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntTimeException(RuntimeException e) {
        Map<String, String> body = new HashMap<>();
        body.put("message", e.getMessage());
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

}
