package com.mall.notification.domain.inbox.repository;

/**
 * 入站事件去重：记录必须与站内欢迎通知的创建位于同一 MySQL 事务。
 */
public interface InboxEventRepository {
    boolean markProcessedIfAbsent(String consumerName, String eventId, String payloadSha256);
}
