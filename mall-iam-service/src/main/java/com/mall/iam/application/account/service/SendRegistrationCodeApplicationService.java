package com.mall.iam.application.account.service;

import com.mall.common.exception.BusinessException;
import com.mall.iam.application.account.port.sms.RegistrationSmsSender;
import com.mall.iam.application.account.port.verification.RegistrationCodeIssueStore;
import com.mall.iam.domain.account.error.AccountErrorCode;
import com.mall.iam.domain.account.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;


@Service
public class SendRegistrationCodeApplicationService {
    private static final Logger log = LoggerFactory.getLogger(SendRegistrationCodeApplicationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final AccountRepository accountRepository;
    private final RegistrationCodeIssueStore issueStore;
    private final RegistrationSmsSender smsSender;
    private final int validSeconds;

    public SendRegistrationCodeApplicationService(
            AccountRepository accountRepository,
            RegistrationCodeIssueStore issueStore,
            RegistrationSmsSender smsSender,
            @Value("${iam.registration.code-expire-seconds:300}") int validSeconds) {
        if (validSeconds < 60) {
            throw new IllegalArgumentException("Verification-code TTL must be at least 60 seconds");
        }
        this.accountRepository = accountRepository;
        this.issueStore = issueStore;
        this.smsSender = smsSender;
        this.validSeconds = validSeconds;
    }


    public void send(String rawPhone) {

        if (rawPhone == null ||
                !PHONE_PATTERN.matcher(rawPhone).matches()) {
            throw new BusinessException(AccountErrorCode.PHONE_FORMAT_INVALID);}
        if (accountRepository.existsByPhone(rawPhone)) {
            throw new BusinessException(AccountErrorCode.PHONE_ALREADY_EXISTS);}

        String token = UUID.randomUUID().toString();

        issueStore.acquire(rawPhone, token);

        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));

        try {
            smsSender.sendRegistrationCode(rawPhone, code, validSeconds);
        } catch (RuntimeException failure) {
            try {
                issueStore.release(rawPhone, token);
            } catch (RuntimeException cleanupFailure) {
                log.warn("Unable to release registration SMS pending slot", cleanupFailure);
            }
            throw failure;
        }

        issueStore.activate(rawPhone, token, code);
    }

}
