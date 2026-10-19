package com.mall.iam.infrastructure.config;

import com.mall.iam.infrastructure.messaging.outbox.OutboxPublisher;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
public class OutboxConfig {
    @Bean public DirectExchange accountExchange() {
        return new DirectExchange(OutboxPublisher.EXCHANGE, true, false);
    }
    @Bean public Queue accountRegisteredQueue() {
        return new Queue("iam.account.registered", true);
    }
    @Bean public Binding accountRegisteredBinding(Queue accountRegisteredQueue, DirectExchange accountExchange) {
        return BindingBuilder.bind(accountRegisteredQueue).to(accountExchange).with(OutboxPublisher.ROUTING_KEY);
    }
    @Bean("outboxExecutor") public ThreadPoolTaskExecutor outboxExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("outbox-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
