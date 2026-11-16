package com.mall.notification.interfaces.controller;

import com.mall.common.api.Result;
import com.mall.notification.application.notification.result.NotificationPage;
import com.mall.notification.application.notification.service.NotificationQueryApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 对外经 Gateway 路由 /api/notification/**，转发成本服务 /api/messages/**。
 * 只信任 Gateway 写入的 X-Authenticated-Account-Id；必须禁止客户端直连本服务端口。
 */
@RestController
@RequestMapping("/api/messages")
public class NotificationController {
    private final NotificationQueryApplicationService service;

    public NotificationController(NotificationQueryApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public Result<NotificationPage> list(
            @RequestHeader("X-Authenticated-Account-Id") String authenticatedAccountId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        long accountId = accountId(authenticatedAccountId);
        if (page < 1 || page > 10000 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分页参数不合法");
        }
        return Result.success(service.list(accountId, page, size));
    }

    @PostMapping("/{notificationId}/read")
    public Result<Boolean> markRead(
            @RequestHeader("X-Authenticated-Account-Id") String authenticatedAccountId,
            @PathVariable long notificationId) {
        long accountId = accountId(authenticatedAccountId);
        if (notificationId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "通知ID不合法");
        }
        return Result.success(service.markRead(accountId, notificationId));
    }

    private long accountId(String header) {
        if (header == null || !header.matches("[1-9][0-9]{0,18}")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录身份不合法");
        }
    }
}
