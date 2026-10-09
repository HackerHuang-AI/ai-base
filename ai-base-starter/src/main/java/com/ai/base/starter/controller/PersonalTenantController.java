package com.ai.base.starter.controller;

import com.ai.base.application.bo.PersonalTenantUpdateBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.service.UserService;
import com.ai.base.starter.common.Result;
import com.ai.base.starter.vo.PersonalTenantUpdateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/personal/tenants")
public class PersonalTenantController {
    private final UserService userService;

    public PersonalTenantController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody PersonalTenantUpdateVO request) {
        try {
            PersonalTenantUpdateBO update = new PersonalTenantUpdateBO();
            update.setTenantName(request.getTenantName());
            userService.updateCurrentPersonalTenant(update);
            return Result.success(null);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改个人空间名称失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }
}

