
package com.mall.iam.infrastructure.messaging.outbox;

import com.mall.iam.application.outbox.query.OutboxDelivery;
import com.mall.iam.domain.outbox.model.OutboxEventType;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;


@Component
public class OutboxPublisher {


    private final RabbitTemplate template;
    public OutboxPublisher(RabbitTemplate template) {this.template = template;}

    /**
     * 发布 Outbox 事件。
     *
     * 成功条件：
     * 1. Broker 返回 ACK；
     * 2. 消息没有因无法路由而被退回。
     *
     * 不保证消费者已经处理成功。
     */
    public void publish(OutboxDelivery event) throws Exception {
        OutboxEventType eventType = OutboxEventType.valueOf(event.eventType());
        //1根据事件类型查找路由
        String routingKey = resolveRoutingKey(eventType);

        if (routingKey == null) {
            throw new IllegalArgumentException("不支持的 Outbox 事件类型: " + event.eventType());
        }

        // 2. 构造消息属性
        MessageProperties properties = new MessageProperties();

        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        properties.setMessageId(event.eventId());
        properties.setHeader("eventType", event.eventType());

        // 3使用原始 JSON 字节，避免重复序列化
        Message message = new Message(
                event.payload().getBytes(StandardCharsets.UTF_8),
                properties
        );

        // 4关联发布确认
        CorrelationData correlation = new CorrelationData(event.eventId());

        // 5发送到RabbitMQ
        template.send(IamRabbitRouting.EXCHANGE, routingKey, message, correlation);

        //6等待Broker发布确认
        CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);

        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ Broker NACK: " + confirm.getReason());
        }

        //7检查消息是否成功路由到队列
        if (correlation.getReturned() != null) {
            throw new IllegalStateException("RabbitMQ 消息路由失败: " + correlation.getReturned().getReplyText());
        }
    }

    private String resolveRoutingKey(OutboxEventType eventType) {
        return switch (eventType) {
            case ACCOUNT_REGISTERED -> IamRabbitRouting.ACCOUNT_REGISTERED;
            case ACCOUNT_PASSWORD_CHANGED -> IamRabbitRouting.ACCOUNT_PASSWORD_CHANGED;
            case ROLE_CREATED -> IamRabbitRouting.ROLE_CREATED;
            case PERMISSION_GRANTED -> IamRabbitRouting.PERMISSION_GRANTED;
        };
    }

}
