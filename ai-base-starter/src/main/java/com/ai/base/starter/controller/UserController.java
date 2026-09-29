package com.ai.base.starter.controller;

import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.service.UserService;
import com.ai.base.application.model.user.UserPageResult;
import com.ai.base.application.model.user.UserProfile;
import com.ai.base.application.model.user.UserQuery;
import com.ai.base.starter.common.Result;
import com.ai.base.starter.vo.UserQueryVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 根据全局用户 ID 查询单个用户的基础资料。
     *
     * @param request 用户查询条件，其中 userId 必填
     */
    @PostMapping("/profile")
    public Result<UserProfile> getProfile(@Valid @RequestBody UserQueryVO request) {
        try {
            UserProfile profile = userService.getProfile(toQuery(request));
            log.info("查询用户信息成功, userId={}", request.getUserId());
            return Result.success(profile);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询用户信息失败, userId={}", request.getUserId(), e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 分页查询用户信息，支持用户基本资料、工号、头像地址的模糊匹配，
     * 并可按用户状态和创建时间区间过滤。
     *
     * @param request 分页与筛选条件
     */
    @PostMapping("/page")
    public Result<UserPageResult> page(@Valid @RequestBody UserQueryVO request) {
        try {
            UserPageResult page = userService.page(toQuery(request));
            log.info("分页查询用户信息成功, pageNo={}, pageSize={}, total={}",
                    request.getPageNo(), request.getPageSize(), page.getTotal());
            return Result.success(page);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("分页查询用户信息失败, pageNo={}, pageSize={}", request.getPageNo(), request.getPageSize(), e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    private UserQuery toQuery(UserQueryVO request) {
        UserQuery query = new UserQuery();
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

