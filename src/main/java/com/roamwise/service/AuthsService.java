package com.roamwise.service;

import com.roamwise.dto.SignupRequest;
import com.roamwise.entity.User;
import com.roamwise.repository.UserRepository;
import com.roamwise.utills.JwtUtil;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwts;



    public void signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already Exists");
        }

        String hash = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(hash);
        userRepository.save(user);

    }

    public String login(String email, String password) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User is not registered"));
        Boolean isPasswordMatched = passwordEncoder.matches(password, user.getPasswordHash());
        if (!isPasswordMatched) {
            throw new RuntimeException("Password is Incorrect");
        }
        return jwts.generateToken(email);
    }
}
