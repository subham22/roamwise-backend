package com.roamwise.utills;

import com.roamwise.entity.User;
import com.roamwise.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;

@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    Integer max_limit = 5;

    private record Rule(String name, int limit, Duration window, boolean perUser) {}
    private static final Map<String, Rule> RULES = Map.of(
            "POST /api/trips/generate",      new Rule("generate", 5,  Duration.ofDays(1),    true),
            "POST /api/auth/login",          new Rule("login",    10, Duration.ofMinutes(1), false),
            "POST /api/auth/signup",         new Rule("signup",   5,  Duration.ofHours(1),   false),
            "POST /api/auth/forgot-password", new Rule("forgot",  3,  Duration.ofHours(1),   false)
    );

    private final RateLimitService rateLimitService;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Rule rule = RULES.get(request.getMethod() + " " + request.getRequestURI());
        if (rule == null) {
            return true;
        }

        String who =  rule.perUser() ? "user" : "user:" + currentUserId() + "ip:" + clientIp(request);
        String key = "rl:" + rule.name() + ":" + who;

        boolean allowed = rateLimitService.tryConsume(key, rule.limit(), rule.window());

        if (allowed) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(rule.window().toSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}");
        return false;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded  = request.getHeader("X-Forwarded-For");
        if (forwarded  != null && !forwarded .isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length-1].trim();
        }
        return request.getRemoteAddr();
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();;
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return String.valueOf(user.getId());
        }
        return "anonymous";
    }
}