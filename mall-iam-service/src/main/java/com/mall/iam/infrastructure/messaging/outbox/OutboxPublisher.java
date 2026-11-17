
package com.mall.iam.infrastructure.messaging.outbox;

import com.mall.iam.application.outbox.query.OutboxDelivery;
import com.mall.iam.domain.outbox.model.OutboxEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Component
public class OutboxPublisher {
    // Notification Service 实际监听的队列名称

    private static final Logger log =  LoggerFactory.getLogger(OutboxPublisher.class);

    private static final long CONFIRM_TIMEOUT_SECONDS = 10;

    private final RabbitTemplate template;

    public OutboxPublisher(RabbitTemplate template) {this.template = template;}

    /**
     * 发布 Outbox 事件。
     * 成功条件：
     * 1. Broker 返回 ACK；
     * 2. 消息没有因无法路由而被退回。
     * 不保证消费者已经处理成功。
     */
    public void publish(OutboxDelivery event) throws Exception {
        //1校验待发送的 Outbox 事件
        validateEvent(event);
        OutboxEventType eventType = OutboxEventType.valueOf(event.eventType());
        //2根据事件类型确定 Routing Key
        String routingKey = resolveRoutingKey(eventType);
        if (routingKey == null) {throw new IllegalArgumentException("不支持的 Outbox 事件类型: " + event.eventType());}
        //3构造消息属性
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(event.eventId());
        properties.setHeader("eventType", event.eventType());
        //4保留原始JON消息体
        Message message = new Message(event.payload().getBytes(StandardCharsets.UTF_8), properties);
        //5创建当前发布尝试的确认标识，SpringAMQP关联本次发送的ACK/NACK/Return，每次尝试必须变
        CorrelationData correlation = new CorrelationData(event.eventId() + ":" + UUID.randomUUID());
        //6发送到RabbitMQ
        template.send(IamRabbitRouting.EXCHANGE, routingKey, message, correlation);
        //7等待Broker发布确认
        CorrelationData.Confirm confirm;
        try {
            confirm = correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("RabbitMQ发布确认等待被中断，eventId=" + event.eventId(), e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("RabbitMQ发布确认超时，投递结果不明确，eventId=" + event.eventId(), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("RabbitMQ发布确认异常，eventId=" + event.eventId(), e);
        }
        //8校验 Broker ACK
        if (!confirm.isAck()) {
            throw new IllegalStateException(
                    "RabbitMQ Broker NACK，eventId=" + event.eventId()
                            + "，reason=" + confirm.getReason()
            );
        }
        //9检查消息是否因无法路由而被退回
        ReturnedMessage returned = correlation.getReturned();
        if (returned != null) {
            throw new IllegalStateException(
                    "RabbitMQ消息无法路由，eventId=" + event.eventId()
                            + "，exchange=" + returned.getExchange()
                            + "，routingKey=" + returned.getRoutingKey()
                            + "，replyCode=" + returned.getReplyCode()
                            + "，replyText=" + returned.getReplyText()
            );
        }
        //10Broker 已确认且未发生无法路由的 Return
        log.debug(
                "Outbox事件发布确认成功，eventId={}，eventType={}，exchange={}，routingKey={}",
                event.eventId(),
                event.eventType(),
                IamRabbitRouting.EXCHANGE,
                routingKey
        );
    }
    private void validateEvent(OutboxDelivery event) {
        Objects.requireNonNull(event, "Outbox事件不能为空");
        if (event.eventId() == null || event.eventId().isBlank()) {throw new IllegalArgumentException("Outbox事件ID不能为空");}
        if (event.eventType() == null || event.eventType().isBlank()) {throw new IllegalArgumentException("Outbox事件类型不能为空");}
        if (event.payload() == null || event.payload().isBlank()) {throw new IllegalArgumentException("Outbox消息体不能为空");}
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
