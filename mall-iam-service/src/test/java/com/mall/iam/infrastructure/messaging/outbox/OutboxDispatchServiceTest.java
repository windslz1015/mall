package com.mall.iam.infrastructure.messaging.outbox;

import com.mall.iam.domain.outbox.model.OutboxDelivery;
import com.mall.iam.domain.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxDispatchServiceTest {
    private final OutboxEventRepository repo = mock(OutboxEventRepository.class);
    private final OutboxPublisher publisher = mock(OutboxPublisher.class);
    private final OutboxDispatchService service = new OutboxDispatchService(
            repo, publisher, mock(ThreadPoolTaskExecutor.class));

    @Test void onlyClaimWinnerCanPublish() {
        when(repo.claim(eq(99L), anyString())).thenReturn(false);
        service.dispatch(99L);
        verifyNoInteractions(publisher);
    }

    @Test void ackMarksSent() throws Exception {
        when(repo.claim(eq(99L), anyString())).thenReturn(true);
        when(repo.findClaimed(eq(99L), anyString()))
                .thenReturn(Optional.of(new OutboxDelivery(99L, "event-1", "AccountRegistered", "{}", 0)));
        service.dispatch(99L);
        verify(publisher).publish(any(OutboxDelivery.class));
        verify(repo).markSent(eq(99L), anyString());
    }

    @Test void publisherFailureSchedulesRetry() throws Exception {
        when(repo.claim(eq(99L), anyString())).thenReturn(true);
        when(repo.findClaimed(eq(99L), anyString()))
                .thenReturn(Optional.of(new OutboxDelivery(99L, "event-1", "AccountRegistered", "{}", 0)));
        doThrow(new IllegalStateException("broker down")).when(publisher).publish(any());
        service.dispatch(99L);
        verify(repo).markFailed(eq(99L), anyString(), any());
        verify(repo, never()).markSent(anyLong(), anyString());
    }
}
