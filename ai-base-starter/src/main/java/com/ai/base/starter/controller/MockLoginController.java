package com.ai.base.starter.controller;

import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.auth.LoginResult;
import com.ai.base.application.auth.SessionInfo;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.service.LoginService;
import com.ai.base.application.service.SmsCodeService;
import com.ai.base.starter.common.Result;
import com.ai.base.starter.vo.LoginRequestVO;
import com.ai.base.starter.vo.LogoutSessionRequestVO;
import com.ai.base.starter.vo.SendSmsCodeRequestVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/access")
public class MockLoginController {
    private final LoginService loginService;
    private final SmsCodeService smsCodeService;

    public MockLoginController(LoginService loginService, SmsCodeService smsCodeService) {
        this.loginService = loginService;
        this.smsCodeService = smsCodeService;
    }

    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequestVO request, HttpServletRequest servletRequest) {
        try {
            LoginCommand command = new LoginCommand();
            command.setLoginType(request.getLoginType());
            command.setAccount(request.getAccount());
            command.setPassword(request.getPassword());
            command.setMobile(request.getMobile());
            command.setSmsCode(request.getSmsCode());
            command.setQrLoginId(request.getQrLoginId());
            command.setDeviceId(request.getDeviceId());
            command.setLoginIp(resolveClientIp(servletRequest));
            command.setUserAgent(servletRequest.getHeader("User-Agent"));
            return Result.success(loginService.login(command));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("登录失败, loginType={}", request.getLoginType(), e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @GetMapping("/sessions")
    public Result<List<SessionInfo>> sessions() {
        try {
            return Result.success(loginService.listSessions());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询登录会话失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        try {
            loginService.logout();
            return Result.success(null);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("退出登录失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @PostMapping("/sessions/logout")
    public Result<Void> logoutSession(@Valid @RequestBody LogoutSessionRequestVO request) {
        try {
            loginService.logout(request.getSessionId());
            return Result.success(null);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("下线指定会话失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @PostMapping("/logout-all")
    public Result<Void> logoutAll() {
        try {
            loginService.logoutAll();
            return Result.success(null);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("退出全部登录会话失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @PostMapping("/sms-code/send")
    public Result<Void> sendSmsCode(@Valid @RequestBody SendSmsCodeRequestVO request) {
        try {
            smsCodeService.sendLoginCode(request.getMobile());
            log.info("发送登录验证码成功");
            return Result.success(null);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("发送登录验证码失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String clientIp = forwardedFor.split(",", 2)[0].trim();
            if (StringUtils.hasText(clientIp)) {
                return clientIp;
            }
        }
        return request.getRemoteAddr();
    }
}

