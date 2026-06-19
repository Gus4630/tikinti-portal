package az.tikinti.portal.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${application.jwt.access-secret}")
    private String accessSecret;

    @Value("${application.jwt.access-expiry-minutes:15}")
    private long accessExpiryMinutes;

    public String generateAccessToken(UUID userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpiryMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessExpirySeconds() {
        return accessExpiryMinutes * 60;
    }

    public boolean isTokenValid(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return parseAccessToken(token).getSubject();
    }

    public String extractRole(String token) {
        return parseAccessToken(token).get("role", String.class);
    }
}
