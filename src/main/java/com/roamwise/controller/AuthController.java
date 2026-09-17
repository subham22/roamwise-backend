package com.roamwise.controller;


import com.roamwise.dto.LoginRequest;
import com.roamwise.dto.SignupRequest;
import com.roamwise.entity.User;
import com.roamwise.repository.UserRepository;
import com.roamwise.service.PlacesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.roamwise.service.AuthsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthsService authService;
    private final UserRepository userRepository;


    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User Created!!");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
    if (!userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User Not Registered !!");
        }

        String token = authService.login(request.getEmail(), request.getPassword());
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok().body(response);

    }
}
