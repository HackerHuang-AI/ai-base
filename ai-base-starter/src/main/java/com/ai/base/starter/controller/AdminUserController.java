package com.ai.base.starter.controller;

import com.ai.base.application.bo.AdminUserQueryBO;
import com.ai.base.application.bo.BasePageOutBO;
import com.ai.base.application.bo.UserProfileBO;
import com.ai.base.application.bo.UserWithTenantsBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.service.UserService;
import com.ai.base.starter.common.Result;
import com.ai.base.starter.vo.AdminUserPageVO;
import com.ai.base.starter.vo.AdminUserProfilesVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/page")
    public Result<BasePageOutBO<UserProfileBO>> adminUserPage(@Valid @RequestBody AdminUserPageVO request) {
        try {
            return Result.success(userService.adminUserPage(toQuery(request)));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("分页查询用户信息失败, pageNo={}, pageSize={}", request.getPageNo(), request.getPageSize(), e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @PostMapping("/profiles")
    public Result<List<UserProfileBO>> listProfiles(@Valid @RequestBody AdminUserProfilesVO request) {
        try {
            return Result.success(userService.listProfiles(request.getUserIds()));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量查询用户信息失败, userIds={}", request.getUserIds(), e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @PostMapping("/profiles-with-tenants")
    public Result<List<UserWithTenantsBO>> listProfilesWithTenants(@Valid @RequestBody AdminUserProfilesVO request) {
        try {
            return Result.success(userService.listProfilesWithTenants(request.getUserIds()));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量查询用户及所属租户失败, userIds={}", request.getUserIds(), e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    private AdminUserQueryBO toQuery(AdminUserPageVO request) {
        AdminUserQueryBO query = new AdminUserQueryBO();
        query.setUserId(request.getUserId());
        query.setMobile(request.getMobile());
        query.setEmail(request.getEmail());
        query.setUsername(request.getUsername());
        query.setName(request.getName());
        query.setAvatarUrl(request.getAvatarUrl());
        query.setJobNumber(request.getJobNumber());
        query.setStatus(request.getStatus());
        query.setCreatedStartTime(request.getCreatedStartTime());
        query.setCreatedEndTime(request.getCreatedEndTime());
        query.setPageNo(request.getPageNo());
        query.setPageSize(request.getPageSize());
        return query;
    }
}

