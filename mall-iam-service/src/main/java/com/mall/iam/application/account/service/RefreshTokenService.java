package com.mall.iam.application.account.service;

import com.mall.common.exception.BusinessException;
import com.mall.iam.domain.account.error.AccountErrorCode;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate(String sessionId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return sessionId + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String sessionId(String refreshToken) {
        if (refreshToken == null || refreshToken.length() > 128) invalid();
        String[] parts = refreshToken.split("\\.", -1);
        if (parts.length != 2 || parts[1].length() != 43) invalid();
        try {
            UUID uuid = UUID.fromString(parts[0]);
            if (!uuid.toString().equals(parts[0])) invalid();
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            if (decoded.length != 32) invalid();
        } catch (IllegalArgumentException e) { invalid(); }
        return parts[0];
    }

    public String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private void invalid() { throw new BusinessException(AccountErrorCode.LOGIN_REFRESH_INVALID); }
}
