package com.mall.iam.interfaces.controller;

import com.mall.common.api.Result;
import com.mall.iam.application.account.command.RegisterAccountCommand;
import com.mall.iam.application.account.result.RegisterAccountResult;
import com.mall.iam.application.account.service.RegisterAccountApplicationService;
import com.mall.iam.interfaces.controller.dto.RegisterRequest;
import com.mall.iam.interfaces.controller.dto.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final RegisterAccountApplicationService registerService;
    public AuthController(RegisterAccountApplicationService registerService) {
        this.registerService = registerService;
    }

    @GetMapping("/ping")
    public Result<String> register() {
        return Result.success("pong");
    }

    @PostMapping("/register")
    public Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {

        RegisterAccountCommand command = new RegisterAccountCommand(request.username(), request.password(), request.phone(), request.email(), request.nickname());

        RegisterAccountResult result = registerService.register(command);

        RegisterResponse response = new RegisterResponse(result.accountId(), result.accountNo(), result.username());

        return Result.success(response);
    }
}
