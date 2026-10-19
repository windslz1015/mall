package com.mall.iam.application.account.service;

import com.mall.common.exception.BusinessException;
import com.mall.iam.application.account.command.RegisterAccountCommand;
import com.mall.iam.application.account.port.event.RegistrationEventDispatcher;
import com.mall.iam.application.account.port.verification.RegistrationCodeVerifier;
import com.mall.iam.application.account.result.RegisterAccountResult;
import com.mall.iam.domain.account.repository.AccountRepository;
import com.mall.iam.domain.account.service.PasswordPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegisterAccountApplicationServiceTest {
    private final RegisterAccountTransactionService tx = mock(RegisterAccountTransactionService.class);
    private final RegistrationCodeVerifier verifier = mock(RegistrationCodeVerifier.class);
    private final AccountRepository repository = mock(AccountRepository.class);
    private final RegistrationEventDispatcher dispatcher = mock(RegistrationEventDispatcher.class);
    private RegisterAccountApplicationService service;
    private final RegisterAccountCommand command = new RegisterAccountCommand(
            "alice01", "Test@123456", "13812345678", null, "Alice", "123456");

    @BeforeEach void setup() {
        reset(tx, verifier, repository, dispatcher);
        service = new RegisterAccountApplicationService(tx, verifier, new PasswordPolicy(), repository, dispatcher);
        when(verifier.reserve("13812345678", "123456")).thenReturn("token-123");
    }

    @Test void successConsumesCodeOnlyAfterTxServiceReturns() {
        RegisterAccountResult result = new RegisterAccountResult(1L, "AC123", "alice01");
        when(tx.createAccount(command)).thenReturn(new RegistrationWriteResult(result, 77L));
        assertEquals(result, service.register(command));
        var order = inOrder(verifier, tx, dispatcher);
        order.verify(verifier).reserve("13812345678", "123456");
        order.verify(tx).createAccount(command);
        order.verify(verifier).confirm("13812345678", "token-123", "123456");
        order.verify(dispatcher).submit(77L);
        verify(verifier, never()).release(anyString(), anyString());
    }

    @Test void dbFailureReleasesLeaseAndNeverPublishes() {
        when(tx.createAccount(command)).thenThrow(new IllegalStateException("profile insert failed"));
        assertThrows(IllegalStateException.class, () -> service.register(command));
        verify(verifier).release("13812345678", "token-123");
        verify(verifier, never()).confirm(anyString(), anyString(), anyString());
        verifyNoInteractions(dispatcher);
    }

    @Test void uniqueIndexCollisionMapsToUsernameError() {
        when(tx.createAccount(command)).thenThrow(new DuplicateKeyException("uk_username"));
        when(repository.existsByUsername("alice01")).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.register(command));
        assertEquals(10001, ex.getCode());
        verify(verifier).release("13812345678", "token-123");
    }

    @Test void redisPostCommitFailureDoesNotTurnSuccessIntoFailure() {
        RegisterAccountResult result = new RegisterAccountResult(1L, "AC123", "alice01");
        when(tx.createAccount(command)).thenReturn(new RegistrationWriteResult(result, 77L));
        doThrow(new IllegalStateException("Redis down")).when(verifier).confirm(anyString(), anyString(), anyString());
        assertEquals(result, service.register(command));
        verify(dispatcher).submit(77L);
    }

    @Test void malformedPasswordDoesNotConsumeCode() {
        RegisterAccountCommand invalid = new RegisterAccountCommand("alice01", "123", "13812345678", null, null, "123456");
        assertThrows(BusinessException.class, () -> service.register(invalid));
        verifyNoInteractions(verifier, tx, dispatcher);
    }
}
