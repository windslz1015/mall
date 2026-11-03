package com.mall.iam.infrastructure.messaging.outbox;

import com.mall.iam.domain.outbox.model.OutboxDelivery;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Publisher confirm + mandatory returns；消息仍采用至少一次投递语义。 */
@Component
public class OutboxPublisher {
    public static final String EXCHANGE = "iam.account.events";
    public static final String ROUTING_KEY = "account.registered";
    private final RabbitTemplate template;
    public OutboxPublisher(RabbitTemplate template) { this.template = template; }

    public void publish(OutboxDelivery event) throws Exception {
        CorrelationData correlation = new CorrelationData(event.eventId());
        template.convertAndSend(EXCHANGE, ROUTING_KEY, event.payload(), message -> {
            message.getMessageProperties().setContentType("application/json");
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setMessageId(event.eventId());
            message.getMessageProperties().setHeader("eventType", event.eventType());
            return message;
        }, correlation);
        CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ broker NACK: " + confirm.getReason());
        }
        if (correlation.getReturned() != null) {
            throw new IllegalStateException("RabbitMQ 路由失败，消息被退回");
        }
    }
}
