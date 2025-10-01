package com.fortify.report.ssc;


import com.fortify.report.service.ReportConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Service
public class SscAuthService {

    private final RestClient http;
    private final String headerPrefix;

    // Configured long-lived PAT (if available)
    private final String configuredToken;

    // Fallback creds for programmatic login
    private final String username;
    private final String password;

    // Configurable obtain-token endpoint path (SSC versions may differ)
    private final String obtainPath;

    // Refresh a little before expiry (seconds)
    private final long refreshSkewSeconds;

    // Cached short-lived token
    private volatile String cachedToken;
    private volatile Instant expiresAt;
    private static final Logger log = LoggerFactory.getLogger(SscAuthService.class);
    public SscAuthService(
            @Value("${fortify.ssc.baseUrl}") String baseUrl,
            @Value("${fortify.ssc.authHeaderPrefix:FortifyToken}") String headerPrefix,
            @Value("${fortify.ssc.token:}") String configuredToken,
            @Value("${fortify.ssc.username:}") String username,
            @Value("${fortify.ssc.password:}") String password,
            @Value("${fortify.ssc.auth.obtainPath:/api/v1/auth/obtain_token}") String obtainPath,
            @Value("${fortify.ssc.auth.refreshSkewSeconds:60}") long refreshSkewSeconds
    ) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
        this.headerPrefix = headerPrefix;
        this.configuredToken = configuredToken;
        this.username = username;
        this.password = password;
        this.obtainPath = obtainPath;
        this.refreshSkewSeconds = refreshSkewSeconds;
    }

    /** Returns a valid token (PAT if set; otherwise logs in and caches a session token). */
    public String token() {
        // Prefer personal access token if provided
        if (configuredToken != null && !configuredToken.isBlank()) {
            return configuredToken;
        }
        // Double-checked locking to avoid sync on every call
        if (cachedToken == null || isExpiredSoon()) {
            synchronized (this) {
                if (cachedToken == null || isExpiredSoon()) {
                    obtainAndCacheSessionToken();
                }
            }
        }
        return cachedToken;
    }

    /** Authorization header value to use in requests. */
    public String authHeaderValue() {
        return headerPrefix + " " + token();
    }

    /** Force refresh (e.g., on 401). */
    public synchronized void invalidate() {
        cachedToken = null;
        expiresAt = null;
    }

    // -------- internals --------

    private boolean isExpiredSoon() {
        return expiresAt == null || Instant.now().isAfter(expiresAt.minusSeconds(refreshSkewSeconds));
    }

    @SuppressWarnings("unchecked")
    private void obtainAndCacheSessionToken() {
        if (isBlank(username) || isBlank(password)) {
            throw new IllegalStateException(
                    "SSC token not set and username/password not provided for programmatic login");
        }

        Map<String, Object> body = Map.of("userName", username, "password", password);

        // Use a typed response to avoid wildcard map issues
        Map<String, Object> res = http.post()
                .uri(obtainPath)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        // SSC responses vary by version. Try common shapes:

        // 1) { "data": { "token": "...", "expires": 1700000000000 } }
        Map<String, Object> data = safeMap(res.get("data"));

        String token = strOrNull(data.get("token"));
        if (isBlank(token)) {
            // 2) { "token": "...", "expires": ... } (top-level)
            token = strOrNull(res.get("token"));
        }
        if (isBlank(token)) {
            throw new IllegalStateException("SSC did not return a token");
        }

        // expiry: prefer millis 'expires', else seconds 'expiresIn', else default 30 min
        Long expiresMillis = longOrNull(data.get("expires"));
        if (expiresMillis == null) expiresMillis = longOrNull(res.get("expires"));

        Long expiresInSeconds = longOrNull(data.get("expiresIn"));
        if (expiresInSeconds == null) expiresInSeconds = longOrNull(res.get("expiresIn"));

        if (expiresMillis != null) {
            expiresAt = Instant.ofEpochMilli(expiresMillis);
        } else if (expiresInSeconds != null) {
            expiresAt = Instant.now().plusSeconds(expiresInSeconds);
        } else {
            // conservative default if SSC doesn’t tell us
            expiresAt = Instant.now().plusSeconds(30 * 60);
        }

        cachedToken = token;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeMap(Object obj) {
        return (obj instanceof Map) ? (Map<String, Object>) obj : Map.of();
    }

    private static String strOrNull(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }

    private static Long longOrNull(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
