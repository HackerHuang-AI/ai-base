package com.ai.base.starter.controller;

import com.ai.base.application.bo.UserTenantPreferenceBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.service.UserTenantPreferenceService;
import com.ai.base.starter.common.Result;
import com.ai.base.starter.vo.UserTenantPreferenceUpdateVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user/tenant-preference")
public class UserTenantPreferenceController {
    private final UserTenantPreferenceService preferenceService;

    public UserTenantPreferenceController(UserTenantPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public Result<UserTenantPreferenceBO> getCurrent() {
        try {
            return Result.success(preferenceService.getCurrent());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询当前用户空间偏好失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody UserTenantPreferenceUpdateVO request) {
        try {
            preferenceService.updateCurrentDefault(request.getDefaultTenantId());
            return Result.success(null);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改当前用户默认空间失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }
}

