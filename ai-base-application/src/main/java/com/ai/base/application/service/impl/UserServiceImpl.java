package com.ai.base.application.service.impl;

import com.ai.base.application.bo.*;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.IdentityProviderEnum;
import com.ai.base.application.enums.IdentityTypeEnum;
import com.ai.base.application.port.TenantAccountPort;
import com.ai.base.application.port.UserAccountPort;
import com.ai.base.application.port.UserProfileQueryPort;
import com.ai.base.application.port.UserTenantRelationPort;
import com.ai.base.application.service.LoginService;
import com.ai.base.application.service.SmsCodeService;
import com.ai.base.application.service.UserService;
import com.ai.base.application.utils.CurrentUserUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private final UserAccountPort userAccountPort;
    private final UserProfileQueryPort userProfileQueryPort;
    private final UserTenantRelationPort userTenantRelationPort;
    private final TenantAccountPort tenantAccountPort;
    private final SmsCodeService smsCodeService;
    private final LoginService loginService;

    public UserServiceImpl(UserAccountPort userAccountPort,
                           UserProfileQueryPort userProfileQueryPort,
                           UserTenantRelationPort userTenantRelationPort,
                           TenantAccountPort tenantAccountPort,
                           SmsCodeService smsCodeService, LoginService loginService) {
        this.userAccountPort = userAccountPort;
        this.userProfileQueryPort = userProfileQueryPort;
        this.userTenantRelationPort = userTenantRelationPort;
        this.tenantAccountPort = tenantAccountPort;
        this.smsCodeService = smsCodeService;
        this.loginService = loginService;
    }

    @Override
    public PersonalUserProfileBO getCurrentProfile() {
        String userId = CurrentUserUtil.requireUserId();
        PersonalUserProfileBO result = new PersonalUserProfileBO();
        copyProfile(getProfile(userId), result);
        UserTenantBO tenant = userTenantRelationPort.findPersonalRelation(userId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.PERSONAL_TENANT_NOT_FOUND));
        result.setPersonalTenant(tenant);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentProfile(PersonalUserUpdateBO update) {
        String userId = CurrentUserUtil.requireUserId();
        UserAccountBO user = userAccountPort.findUser(userId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.USER_NOT_FOUND));
        user.setEmail(update.getEmail());
        user.setName(update.getName());
        if (!userAccountPort.updateAccount(user)) {
            throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
        }
        UserInfoBO userInfo = new UserInfoBO();
        userInfo.setUserId(userId);
        userInfo.setAvatarUrl(update.getAvatarUrl());
        userInfo.setJobNumber(update.getJobNumber());
        userAccountPort.upsertUserInfo(userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentUsername(PersonalUsernameUpdateBO update) {
        if (!StringUtils.hasText(update.getUsername())) {
            throw new BizException(ErrorCodeEnum.PARAMETER_REQUIRED);
        }
        String userId = CurrentUserUtil.requireUserId();
        UserAccountBO user = getActiveUser(userId);
        if (update.getUsername().equals(user.getUsername())) {
            return;
        }
        ensureIdentityAvailable(IdentityTypeEnum.ACCOUNT.getValue(), IdentityProviderEnum.LOCAL.getValue(), update.getUsername());
        try {
            user.setUsername(update.getUsername());
            if (!userAccountPort.updateAccount(user)) {
                throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
            }
            userAccountPort.upsertIdentity(userId, IdentityTypeEnum.ACCOUNT.getValue(),
                    IdentityProviderEnum.LOCAL.getValue(), update.getUsername());
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCodeEnum.CREDENTIAL_ALREADY_IN_USE);
        }
        logoutAllAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentMobile(PersonalMobileUpdateBO update) {
        if (!StringUtils.hasText(update.getMobile()) || !StringUtils.hasText(update.getSmsCode())) {
            throw new BizException(ErrorCodeEnum.PARAMETER_REQUIRED);
        }
        String userId = CurrentUserUtil.requireUserId();
        UserAccountBO user = getActiveUser(userId);
        if (update.getMobile().equals(user.getMobile())) {
            return;
        }
        ensureIdentityAvailable(IdentityTypeEnum.MOBILE.getValue(), IdentityProviderEnum.SMS.getValue(), update.getMobile());
        smsCodeService.consumeLoginCode(update.getMobile(), update.getSmsCode());
        try {
            user.setMobile(update.getMobile());
            if (!userAccountPort.updateAccount(user)) {
                throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
            }
            userAccountPort.upsertIdentity(userId, IdentityTypeEnum.MOBILE.getValue(),
                    IdentityProviderEnum.SMS.getValue(), update.getMobile());
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCodeEnum.CREDENTIAL_ALREADY_IN_USE);
        }
        logoutAllAfterCommit();
    }

    @Override
    public void updateCurrentPersonalTenant(PersonalTenantUpdateBO update) {
        if (!StringUtils.hasText(update.getTenantName())) {
            throw new BizException(ErrorCodeEnum.PARAMETER_REQUIRED);
        }
        String tenantId = userTenantRelationPort.findPersonalTenantId(CurrentUserUtil.requireUserId())
                .orElseThrow(() -> new BizException(ErrorCodeEnum.PERSONAL_TENANT_NOT_FOUND));
        tenantAccountPort.findPersonalTenant(tenantId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.PERSONAL_TENANT_NOT_FOUND));
        tenantAccountPort.renameTenant(tenantId, update.getTenantName());
    }

    @Override
    public List<UserProfileBO> listProfiles(List<String> userIds) {
        validateUserIds(userIds);
        Map<String, UserProfileBO> profilesByUserId = userProfileQueryPort.findProfilesByUserIds(userIds).stream()
                .collect(Collectors.toMap(UserProfileBO::getUserId, Function.identity()));
        return userIds.stream().distinct()
                .map(profilesByUserId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<UserWithTenantsBO> listProfilesWithTenants(List<String> userIds) {
        validateUserIds(userIds);
        Map<String, List<UserTenantBO>> tenantsByUserId = userTenantRelationPort.listRelationsGroupedByUserIds(userIds);
        Map<String, UserProfileBO> profilesByUserId = userProfileQueryPort.findProfilesByUserIds(userIds).stream()
                .collect(Collectors.toMap(UserProfileBO::getUserId, Function.identity()));
        return userIds.stream().distinct()
                .map(profilesByUserId::get)
                .filter(Objects::nonNull)
                .map(profile -> toProfileWithTenants(profile,
                        tenantsByUserId.getOrDefault(profile.getUserId(), List.of())))
                .toList();
    }

    @Override
    public BasePageOutBO<UserProfileBO> adminUserPage(AdminUserQueryBO query) {
        BasePageOutBO<UserProfileBO> result = new BasePageOutBO<>();
        result.setTotal(userProfileQueryPort.countUsers(query));
        result.setPageNo(Math.toIntExact(query.getPageNo()));
        result.setPageSize(Math.toIntExact(query.getPageSize()));
        result.setRecords(userProfileQueryPort.pageUsers(query,
                        (query.getPageNo() - 1) * query.getPageSize(),
                        Math.toIntExact(query.getPageSize())));
        return result;
    }

    private void logoutAllAfterCommit() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                loginService.logoutAll();
            }
        });
    }

    private UserAccountBO getActiveUser(String userId) {
        return userAccountPort.findActiveUser(userId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.USER_NOT_FOUND));
    }

    private void ensureIdentityAvailable(String identityType, String identityProvider, String identityValue) {
        if (userAccountPort.findIdentity(identityType, identityProvider, identityValue).isPresent()) {
            throw new BizException(ErrorCodeEnum.CREDENTIAL_ALREADY_IN_USE);
        }
    }

    private UserProfileBO getProfile(String userId) {
        return userProfileQueryPort.findProfilesByUserIds(List.of(userId)).stream()
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.USER_NOT_FOUND));
    }

    private void validateUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty() || userIds.stream().anyMatch(userId -> !StringUtils.hasText(userId))) {
            throw new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, ErrorCodeEnum.USER_ID_REQUIRED);
        }
    }

    private UserWithTenantsBO toProfileWithTenants(UserProfileBO profile, List<UserTenantBO> tenants) {
        UserWithTenantsBO result = new UserWithTenantsBO();
        copyProfile(profile, result);
        result.setTenants(tenants);
        return result;
    }

    private void copyProfile(UserProfileBO source, UserProfileBO target) {
        target.setUserId(source.getUserId());
        target.setMobile(source.getMobile());
        target.setEmail(source.getEmail());
        target.setUsername(source.getUsername());
        target.setName(source.getName());
        target.setAvatarUrl(source.getAvatarUrl());
        target.setJobNumber(source.getJobNumber());
    }
}
