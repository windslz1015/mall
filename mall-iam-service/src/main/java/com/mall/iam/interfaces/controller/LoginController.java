package com.mall.iam.interfaces.controller;

import com.mall.common.api.Result;
import com.mall.iam.application.account.command.NormalUserLoginCommand;
import com.mall.iam.application.account.result.IssuedLoginTokens;
import com.mall.iam.application.account.result.NormalUserLoginResult;
import com.mall.iam.application.account.service.LoginApplicationService;
import com.mall.iam.application.account.service.RefreshLoginApplicationService;
import com.mall.iam.interfaces.controller.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
    private static final String COOKIE = "mall_refresh";
    // External path seen by the browser, before Gateway RewritePath.
    private static final String COOKIE_PATH = "/api/iam/auth";
    private final LoginApplicationService loginService;
    private final RefreshLoginApplicationService refreshService;
    private final long refreshTtlSeconds;

    public LoginController(LoginApplicationService loginService, RefreshLoginApplicationService refreshService,
            @Value("${iam.jwt.refresh-ttl-seconds:604800}") long refreshTtlSeconds) {
        this.loginService = loginService;
        this.refreshService = refreshService;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    @PostMapping("/login")
    public ResponseEntity<Result<NormalUserLoginResult>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        IssuedLoginTokens issued = loginService.login(new NormalUserLoginCommand(
                request.loginIdentifier(), request.password(), clientIp(servletRequest),
                servletRequest.getHeader(HttpHeaders.USER_AGENT)));
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(issued.refreshToken()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Result.success(issued.response()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Result<NormalUserLoginResult>> refresh(
            @CookieValue(name = COOKIE, required = false) String refreshCookie) {
        IssuedLoginTokens issued = refreshService.refresh(refreshCookie);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(issued.refreshToken()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Result.success(issued.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Result<Void>> logout(
            @CookieValue(name = COOKIE, required = false) String refreshCookie) {
        refreshService.logout(refreshCookie);
        ResponseCookie clear = ResponseCookie.from(COOKIE, "").httpOnly(true).secure(true)
                .sameSite("Strict").path(COOKIE_PATH).maxAge(Duration.ZERO).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, clear.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Result.success());
    }

    private ResponseCookie cookie(String opaqueToken) {
        return ResponseCookie.from(COOKIE, opaqueToken).httpOnly(true).secure(true)
                .sameSite("Strict").path(COOKIE_PATH)
                .maxAge(Duration.ofSeconds(refreshTtlSeconds)).build();
    }

    private String clientIp(HttpServletRequest request) {
        // Only trust this header when IAM is not directly reachable by untrusted clients.
        String gatewayIp = request.getHeader("X-Verified-Client-IP");
        if (gatewayIp != null && gatewayIp.matches("[0-9a-fA-F:.]{1,45}")) return gatewayIp;
        return request.getRemoteAddr();
    }
}
