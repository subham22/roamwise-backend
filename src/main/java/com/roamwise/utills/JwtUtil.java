package com.roamwise.utills;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwt_secret;

    private SecretKey convertToSecretKey() {
        return Keys.hmacShaKeyFor(jwt_secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {

        long nowMillis = System.currentTimeMillis();
        long expiryMillis = nowMillis + (1000L * 60 * 60 * 24); // 24 hours from now

        Date issuedAt = new Date(nowMillis);
        Date expiration = new Date(expiryMillis);

        return Jwts.builder()
                .issuedAt(issuedAt)
                .signWith(this.convertToSecretKey())
                .subject(email)
                .expiration(expiration)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(this.convertToSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }



}
