package com.roamwise.service;

import com.roamwise.dto.TurnstileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Service
public class TurnstileService {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    @Value("${turnstile.secret}")
    private String secret;

    // Comma-separated in properties; e.g. roamwise.live in prod, localhost,example.com in dev
    @Value("${turnstile.allowed-hostnames}")
    private Set<String> allowedHostnames;

    private final RestClient restClient = RestClient.builder()
            .requestFactory(timeouts())
            .build();

    public void verify(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CAPTCHA token missing");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", token);

        TurnstileResponse response;
        try {
            response = restClient.post()
                    .uri(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TurnstileResponse.class);
        } catch (RestClientException e) {
            // Fail closed: if we can't verify, we don't let the request through
            log.error("Turnstile verification call failed", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not verify CAPTCHA, please try again");
        }

        if (response == null || !response.success()) {
            log.warn("Turnstile rejected token: {}", response == null ? "empty response" : response.errorCodes());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CAPTCHA verification failed");
        }

        if (!allowedHostnames.contains(response.hostname())) {
            log.warn("Turnstile token issued for unexpected hostname: {}", response.hostname());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CAPTCHA verification failed");
        }
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }
}