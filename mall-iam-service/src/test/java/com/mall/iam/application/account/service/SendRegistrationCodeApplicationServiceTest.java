package com.mall.iam.application.account.service;

import com.mall.common.exception.BusinessException;
import com.mall.iam.application.account.port.sms.RegistrationSmsSender;
import com.mall.iam.application.account.port.verification.RegistrationCodeIssueStore;
import com.mall.iam.domain.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SendRegistrationCodeApplicationServiceTest {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final RegistrationCodeIssueStore store = mock(RegistrationCodeIssueStore.class);
    private final RegistrationSmsSender sms = mock(RegistrationSmsSender.class);
    private SendRegistrationCodeApplicationService service;

    @BeforeEach void setUp() {
        reset(accounts, store, sms);
        service = new SendRegistrationCodeApplicationService(accounts, store, sms, 300);
    }

    @Test void sendStoresSixDigitCodeOnlyAfterSenderSucceeds() {
        service.send("13812345678");
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        var order = inOrder(accounts, store, sms);
        order.verify(accounts).existsByPhone("13812345678");
        order.verify(store).acquire(eq("13812345678"), token.capture());
        order.verify(sms).sendRegistrationCode(eq("13812345678"), code.capture(), eq(300));
        order.verify(store).activate(eq("13812345678"), eq(token.getValue()), eq(code.getValue()));
        assertTrue(code.getValue().matches("[0-9]{6}"));
        verify(store, never()).release(anyString(), anyString());
    }

    @Test void rejectsAlreadyRegisteredPhoneWithoutSending() {
        when(accounts.existsByPhone("13812345678")).thenReturn(true);
        BusinessException error = assertThrows(BusinessException.class, () -> service.send("13812345678"));
        assertEquals(10002, error.getCode());
        verifyNoInteractions(store, sms);
    }

    @Test void smsFailureReleasesSlotButDoesNotActivateCode() {
        doThrow(new IllegalStateException("SMS unavailable"))
                .when(sms).sendRegistrationCode(anyString(), anyString(), anyInt());
        assertThrows(IllegalStateException.class, () -> service.send("13812345678"));
        verify(store).release(eq("13812345678"), anyString());
        verify(store, never()).activate(anyString(), anyString(), anyString());
    }

    @Test void acquisitionFailureDoesNotContactSmsProvider() {
        doThrow(new IllegalStateException("Rate limit"))
                .when(store).acquire(anyString(), anyString());
        assertThrows(IllegalStateException.class, () -> service.send("13812345678"));
        verifyNoInteractions(sms);
    }

    @Test void invalidPhoneDoesNotQueryDatabase() {
        assertThrows(IllegalArgumentException.class, () -> service.send("not-a-phone"));
        verifyNoInteractions(accounts, store, sms);
    }
}
