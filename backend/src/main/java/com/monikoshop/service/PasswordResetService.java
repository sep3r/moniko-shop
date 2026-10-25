package com.monikoshop.service;

import com.monikoshop.entity.PasswordResetToken;
import com.monikoshop.entity.User;
import com.monikoshop.exception.ApiException;
import com.monikoshop.repository.PasswordResetTokenRepository;
import com.monikoshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long TOKEN_TTL_MINUTES = 30;
    private static final long COOLDOWN_SECONDS = 60;
    private static final String INVALID_TOKEN_MESSAGE = "لینک بازیابی نامعتبر یا منقضی شده است";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Always returns normally, whether or not the e-mail belongs to an account,
     * so the endpoint cannot be used to find out which e-mails are registered.
     */
    @Transactional
    public void requestReset(String email) {
        String normalized = email.trim().toLowerCase();
        Optional<User> found = userRepository.findByEmail(normalized);
        if (found.isEmpty() || !found.get().isEnabled()) {
            return;
        }
        User user = found.get();
        LocalDateTime now = LocalDateTime.now();

        // At most one e-mail per minute per account (prevents e-mail bombing).
        LocalDateTime latest = tokenRepository.findLatestCreatedAt(user.getId());
        if (latest != null && latest.isAfter(now.minusSeconds(COOLDOWN_SECONDS))) {
            return;
        }

        tokenRepository.removeAllForUser(user.getId());
        tokenRepository.removeExpired(now);

        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        tokenRepository.save(PasswordResetToken.builder()
                .tokenHash(sha256Hex(token))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plusMinutes(TOKEN_TTL_MINUTES))
                .build());

        mailService.sendPasswordResetAsync(user.getEmail(), user.getFullName(), token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(sha256Hex(token.trim()))
                .orElseThrow(() -> new ApiException(INVALID_TOKEN_MESSAGE));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(INVALID_TOKEN_MESSAGE);
        }

        User user = resetToken.getUser();
        if (!user.isEnabled()) {
            throw new ApiException(INVALID_TOKEN_MESSAGE);
        }
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException("رمز عبور بیش از حد طولانی است");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // The token is single-use; also drop any other pending tokens of this account.
        tokenRepository.removeAllForUser(user.getId());
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
