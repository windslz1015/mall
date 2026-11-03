package com.mall.iam.infrastructure.sms;

import com.mall.iam.application.account.port.sms.RegistrationSmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** DEV ONLY: print the code for local manual testing. Not a real SMS delivery. */
@Component
@Profile("dev")
public class DevelopmentRegistrationSmsSender implements RegistrationSmsSender {
    private static final Logger log = LoggerFactory.getLogger(DevelopmentRegistrationSmsSender.class);

    @Override
    public void sendRegistrationCode(String phone, String code, int validSeconds) {
        log.warn("[DEV ONLY - NO SMS SENT] registration phone={} code={} ttl={}s",
                phone.substring(0, 3) + "****" + phone.substring(7), code, validSeconds);
    }
}
