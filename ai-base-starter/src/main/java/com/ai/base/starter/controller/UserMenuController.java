package com.ai.base.starter.controller;

import com.ai.base.application.bo.UserMenuBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.service.UserMenuService;
import com.ai.base.starter.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user/menu")
public class UserMenuController {
    private final UserMenuService userMenuService;

    public UserMenuController(UserMenuService userMenuService) {
        this.userMenuService = userMenuService;
    }

    @PostMapping("/tree")
    public Result<List<UserMenuBO>> tree() {
        try {
            return Result.success(userMenuService.treeCurrent());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询当前用户可见导航菜单失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }
}

