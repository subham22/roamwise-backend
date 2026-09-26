package com.roamwise.service;

import com.roamwise.dto.SignupRequest;
import com.roamwise.entity.PasswordResetToken;
import com.roamwise.entity.User;
import com.roamwise.repository.PasswordResetTokenRepository;
import com.roamwise.repository.UserRepository;
import com.roamwise.utills.JwtUtil;
import io.jsonwebtoken.Jwts;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthsService {

    @Value("${app.frontend-url}")
    private String frontendUrl;


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtUtil jwts;
    private final EmailService emailService;

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User is Not Registered"));

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsed(false);
        passwordResetTokenRepository.save(token);

        String resetLink = frontendUrl + "/reset-password?token=" + token.getToken();

        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token) .orElseThrow(() -> new RuntimeException("Invalid or expired reset link"));
        if (resetToken.getUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now()) ) {
            throw new RuntimeException("Invalid or expired reset link");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

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
