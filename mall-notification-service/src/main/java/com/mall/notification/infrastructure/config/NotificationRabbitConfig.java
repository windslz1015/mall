package com.mall.notification.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationRabbitConfig {
    public static final String IAM_EXCHANGE = "iam.events";
    public static final String ACCOUNT_REGISTERED_ROUTING_KEY = "account.registered";
    public static final String DLX = "notification.dlx";
    public static final String DLQ = "notification.account.registered.dlq";

    @Bean
    public TopicExchange iamEventsExchange() {
        return new TopicExchange(IAM_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationAccountRegisteredQueue(
            @Value("${notification.rabbit.account-registered-queue:notification.account.registered}") String queueName) {
        // 为 Notification 自建队列，不和 IAM 或未来其他服务共享同一个消费队列。
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey("registration.failed")
                .build();
    }

    @Bean
    public Binding notificationRegisteredBinding(
            @Qualifier("notificationAccountRegisteredQueue") Queue notificationAccountRegisteredQueue,
            @Qualifier("iamEventsExchange") TopicExchange iamEventsExchange) {
        return BindingBuilder.bind(notificationAccountRegisteredQueue)
                .to(iamEventsExchange).with(ACCOUNT_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange notificationDeadLetterExchange() {
        return new TopicExchange(DLX, true, false);
    }

    @Bean
    public Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding notificationDeadLetterBinding(
            @Qualifier("notificationDeadLetterQueue") Queue notificationDeadLetterQueue,
            @Qualifier("notificationDeadLetterExchange") TopicExchange notificationDeadLetterExchange) {
        return BindingBuilder.bind(notificationDeadLetterQueue)
                .to(notificationDeadLetterExchange).with("registration.failed");
    }
}
