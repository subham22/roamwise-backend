package com.roamwise.utills;

import com.roamwise.entity.User;
import com.roamwise.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private record Rule(String name, int limit, Duration window, boolean perUser) {}

    // "METHOD path" patterns; {id} segments are numeric. Per-user rules apply to logged-in endpoints.
    private static final Map<Pattern, Rule> RULES = Map.of(
            Pattern.compile("POST /api/trips/generate"),           new Rule("generate",  5,  Duration.ofDays(1),    true),
            Pattern.compile("POST /api/trips/\\d+/replan"),        new Rule("replan",    10, Duration.ofDays(1),    true),
            Pattern.compile("POST /api/trips/\\d+/chat-edit"),     new Rule("chat-edit", 20, Duration.ofDays(1),    true),
            Pattern.compile("POST /api/auth/login"),               new Rule("login",     10, Duration.ofMinutes(1), false),
            Pattern.compile("POST /api/auth/signup"),              new Rule("signup",    5,  Duration.ofHours(1),   false),
            Pattern.compile("POST /api/auth/forgot-password"),     new Rule("forgot",    3,  Duration.ofHours(1),   false)
    );

    private final RateLimitService rateLimitService;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String call = request.getMethod() + " " + request.getRequestURI();
        Rule rule = RULES.entrySet().stream()
                .filter(e -> e.getKey().matcher(call).matches())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (rule == null) {
            return true;
        }

        // per-user limits follow the account; anonymous endpoints are limited per client IP
        String who = rule.perUser() ? "user:" + currentUserId() : "ip:" + clientIp(request);
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