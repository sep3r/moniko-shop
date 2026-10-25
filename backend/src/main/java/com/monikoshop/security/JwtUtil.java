package com.monikoshop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    // The old default secret was committed to the repository, so it must never be trusted.
    private static final String LEGACY_DEFAULT_SECRET =
            "YourSuperSecretKeyForJWTTokenGenerationMustBeLongEnough1234567890";

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:28800000}")
    private long expiration;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        String trimmed = secret == null ? "" : secret.trim();
        byte[] keyBytes = trimmed.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32 || LEGACY_DEFAULT_SECRET.equals(trimmed)) {
            keyBytes = new byte[64];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("JWT_SECRET is missing, too short (<32 chars) or the old default. "
                    + "A random temporary secret is used: all users are logged out on every restart. "
                    + "Set JWT_SECRET to a long random value.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /** The token is bound to the current password hash, so changing/resetting the password invalidates old tokens. */
    public String generateToken(UserDetails userDetails, String passwordHash) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("pv", fingerprint(passwordHash));
        return createToken(claims, userDetails.getUsername());
    }

    private static String fingerprint(String passwordHash) {
        if (passwordHash == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        final String passwordVersion = extractClaim(token, claims -> claims.get("pv", String.class));
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && passwordVersion != null
                && passwordVersion.equals(fingerprint(userDetails.getPassword()));
    }
}
