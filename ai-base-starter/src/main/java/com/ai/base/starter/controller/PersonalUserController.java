package com.ai.base.starter.controller;

import com.ai.base.application.bo.PersonalMobileUpdateBO;
import com.ai.base.application.bo.PersonalUserProfileBO;
import com.ai.base.application.bo.PersonalUserUpdateBO;
import com.ai.base.application.bo.PersonalUsernameUpdateBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.service.UserService;
import com.ai.base.starter.common.Result;
import com.ai.base.starter.vo.PersonalMobileUpdateVO;
import com.ai.base.starter.vo.PersonalUserUpdateVO;
import com.ai.base.starter.vo.PersonalUsernameUpdateVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/personal/users")
public class PersonalUserController {
    private final UserService userService;

    public PersonalUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/profile")
    public Result<PersonalUserProfileBO> getProfile() {
        try {
            return Result.success(userService.getCurrentProfile());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询当前用户信息失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @PostMapping("/profile/update")
    public Result<Void> updateProfile(@RequestBody PersonalUserUpdateVO request) {
        try {
            userService.updateCurrentProfile(toUpdate(request));
            return Result.success(null);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改当前用户信息失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @PostMapping("/username/update")
    public Result<Void> updateUsername(@Valid @RequestBody PersonalUsernameUpdateVO request) {
        try {
            PersonalUsernameUpdateBO update = new PersonalUsernameUpdateBO();
            update.setUsername(request.getUsername());
            userService.updateCurrentUsername(update);
            return Result.success(null);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改当前用户账号名失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @PostMapping("/mobile/update")
    public Result<Void> updateMobile(@Valid @RequestBody PersonalMobileUpdateVO request) {
        try {
            PersonalMobileUpdateBO update = new PersonalMobileUpdateBO();
            update.setMobile(request.getMobile());
            update.setSmsCode(request.getSmsCode());
            userService.updateCurrentMobile(update);
            return Result.success(null);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改当前用户手机号失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    private PersonalUserUpdateBO toUpdate(PersonalUserUpdateVO request) {
        PersonalUserUpdateBO update = new PersonalUserUpdateBO();
        update.setEmail(request.getEmail());
        update.setName(request.getName());
        update.setAvatarUrl(request.getAvatarUrl());
        update.setJobNumber(request.getJobNumber());
        return update;
    }
}

