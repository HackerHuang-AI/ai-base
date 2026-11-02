package com.ai.base.starter.controller;

import com.ai.base.application.bo.BasePageOutBO;
import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.service.LoginService;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.starter.common.Result;
import com.ai.base.starter.vo.BasePageInVO;
import com.ai.base.starter.vo.TenantSwitchVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/tenant/user")
public class TenantUserController {
    private final TenantUserService tenantUserService;
    private final LoginService loginService;

    public TenantUserController(TenantUserService tenantUserService, LoginService loginService) {
        this.tenantUserService = tenantUserService;
        this.loginService = loginService;
    }

    @PostMapping("/page")
    public Result<BasePageOutBO<UserTenantBO>> page(@Valid @RequestBody BasePageInVO request) {
        try {
            return Result.success(tenantUserService.pageCurrent(request.getPageNo(), request.getPageSize()));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("分页查询当前用户可用空间失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @PostMapping("/switch")
    public Result<UserTenantBO> switchTenant(@Valid @RequestBody TenantSwitchVO request) {
        try {
            return Result.success(loginService.switchTenant(request.getTenantId()));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("切换当前会话空间失败, tenantId={}", request.getTenantId(), e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }
}

