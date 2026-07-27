package com.mall.user.controller;

import com.mall.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserHealthController {

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("user-service is running");
    }
}
