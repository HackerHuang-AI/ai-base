package com.ai.base.application.service.impl;

import com.ai.base.application.bo.*;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.IdentityProviderEnum;
import com.ai.base.application.enums.IdentityTypeEnum;
import com.ai.base.application.enums.TenantTypeEnum;
import com.ai.base.application.service.LoginService;
import com.ai.base.application.service.SmsCodeService;
import com.ai.base.application.service.UserService;
import com.ai.base.application.utils.CurrentUserUtil;
import com.ai.base.infrastructure.persistence.dto.UserProfileRow;
import com.ai.base.infrastructure.persistence.dto.UserTenantRow;
import com.ai.base.infrastructure.persistence.entity.TenantEntity;
import com.ai.base.infrastructure.persistence.entity.UserEntity;
import com.ai.base.infrastructure.persistence.entity.UserIdentityEntity;
import com.ai.base.infrastructure.persistence.entity.UserInfoEntity;
import com.ai.base.infrastructure.persistence.mapper.TenantMapper;
import com.ai.base.infrastructure.persistence.mapper.UserIdentityMapper;
import com.ai.base.infrastructure.persistence.mapper.UserInfoMapper;
import com.ai.base.infrastructure.persistence.mapper.UserMapper;
import com.ai.base.infrastructure.persistence.mapper.extension.TenantUserExtensionMapper;
import com.ai.base.infrastructure.persistence.mapper.extension.UserExtensionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private final UserExtensionMapper userExtensionMapper;
    private final TenantUserExtensionMapper tenantUserExtensionMapper;
    private final UserMapper userMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserIdentityMapper userIdentityMapper;
    private final TenantMapper tenantMapper;
    private final SmsCodeService smsCodeService;
    private final LoginService loginService;

    public UserServiceImpl(UserExtensionMapper userExtensionMapper,
                           TenantUserExtensionMapper tenantUserExtensionMapper,
                           UserMapper userMapper, UserInfoMapper userInfoMapper,
                           UserIdentityMapper userIdentityMapper, TenantMapper tenantMapper,
                           SmsCodeService smsCodeService, LoginService loginService) {
        this.userExtensionMapper = userExtensionMapper;
        this.tenantUserExtensionMapper = tenantUserExtensionMapper;
        this.userMapper = userMapper;
        this.userInfoMapper = userInfoMapper;
        this.userIdentityMapper = userIdentityMapper;
        this.tenantMapper = tenantMapper;
        this.smsCodeService = smsCodeService;
        this.loginService = loginService;
    }

    @Override
    public PersonalUserProfileBO getCurrentProfile() {
        String userId = CurrentUserUtil.requireUserId();
        PersonalUserProfileBO result = new PersonalUserProfileBO();
        copyProfile(getProfile(userId), result);
        UserTenantRow tenant = tenantUserExtensionMapper.selectPersonalByUserId(userId);
        if (tenant == null) {
            throw new BizException(ErrorCodeEnum.PERSONAL_TENANT_NOT_FOUND);
        }
        result.setPersonalTenant(toTenant(tenant));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentProfile(PersonalUserUpdateBO update) {
        String userId = CurrentUserUtil.requireUserId();
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, userId));
        if (user == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        user.setEmail(update.getEmail());
        user.setName(update.getName());
        if (userMapper.updateById(user) == 0) {
            throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
        }

        UserInfoEntity userInfo = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfoEntity>()
                .eq(UserInfoEntity::getUserId, userId));
        if (userInfo == null) {
            userInfo = new UserInfoEntity();
            userInfo.setUserId(userId);
            userInfo.setAvatarUrl(update.getAvatarUrl());
            userInfo.setJobNumber(update.getJobNumber());
            if (userInfoMapper.insert(userInfo) == 0) {
                throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
            }
        } else {
            userInfo.setAvatarUrl(update.getAvatarUrl());
            userInfo.setJobNumber(update.getJobNumber());
            if (userInfoMapper.updateById(userInfo) == 0) {
                throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
            }
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentUsername(PersonalUsernameUpdateBO update) {
        if (!StringUtils.hasText(update.getUsername())) {
            throw new BizException(ErrorCodeEnum.PARAMETER_REQUIRED);
        }
        String userId = CurrentUserUtil.requireUserId();
        UserEntity user = getActiveUser(userId);
        if (update.getUsername().equals(user.getUsername())) {
            return;
        }
        ensureIdentityAvailable(IdentityTypeEnum.ACCOUNT.getValue(), IdentityProviderEnum.LOCAL.getValue(), update.getUsername());
        try {
            user.setUsername(update.getUsername());
            if (userMapper.updateById(user) == 0) {
                throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
            }
            updateIdentity(userId, IdentityTypeEnum.ACCOUNT.getValue(), IdentityProviderEnum.LOCAL.getValue(), update.getUsername());
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
        UserEntity user = getActiveUser(userId);
        if (update.getMobile().equals(user.getMobile())) {
            return;
        }
        ensureIdentityAvailable(IdentityTypeEnum.MOBILE.getValue(), IdentityProviderEnum.SMS.getValue(), update.getMobile());
        smsCodeService.consumeLoginCode(update.getMobile(), update.getSmsCode());
        try {
            user.setMobile(update.getMobile());
            if (userMapper.updateById(user) == 0) {
                throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
            }
            updateIdentity(userId, IdentityTypeEnum.MOBILE.getValue(), IdentityProviderEnum.SMS.getValue(), update.getMobile());
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
        String tenantId = tenantUserExtensionMapper.selectPersonalTenantId(CurrentUserUtil.requireUserId());
        TenantEntity tenant = tenantId == null ? null : tenantMapper.selectOne(new LambdaQueryWrapper<TenantEntity>()
                .eq(TenantEntity::getTenantId, tenantId)
                .eq(TenantEntity::getTenantType, TenantTypeEnum.PERSONAL.getValue())
                .eq(TenantEntity::getStatus, 1));
        if (tenant == null) {
            throw new BizException(ErrorCodeEnum.PERSONAL_TENANT_NOT_FOUND);
        }
        tenant.setTenantName(update.getTenantName());
        if (tenantMapper.updateById(tenant) == 0) {
            throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
        }
    }

    @Override
    public List<UserProfileBO> listProfiles(List<String> userIds) {
        validateUserIds(userIds);
        Map<String, UserProfileRow> profilesByUserId = userExtensionMapper.selectByUserIds(userIds).stream()
                .collect(Collectors.toMap(UserProfileRow::getUserId, Function.identity()));
        return userIds.stream().distinct()
                .map(profilesByUserId::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toProfile)
                .toList();
    }

    @Override
    public List<UserWithTenantsBO> listProfilesWithTenants(List<String> userIds) {
        validateUserIds(userIds);
        Map<String, List<UserTenantBO>> tenantsByUserId = tenantUserExtensionMapper.selectByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(UserTenantRow::getUserId,
                        Collectors.mapping(this::toTenant, Collectors.toList())));
        Map<String, UserProfileRow> profilesByUserId = userExtensionMapper.selectByUserIds(userIds).stream()
                .collect(Collectors.toMap(UserProfileRow::getUserId, Function.identity()));
        return userIds.stream().distinct()
                .map(profilesByUserId::get)
                .filter(java.util.Objects::nonNull)
                .map(row -> toProfileWithTenants(row, tenantsByUserId))
                .toList();
    }

    @Override
    public BasePageOutBO<UserProfileBO> adminUserPage(AdminUserQueryBO query) {
        UserEntity user = toUserEntity(query);
        UserInfoEntity userInfo = toUserInfoEntity(query);
        BasePageOutBO<UserProfileBO> result = new BasePageOutBO<>();
        result.setTotal(userExtensionMapper.countByQuery(user, userInfo, query.getCreatedEndTime()));
        result.setPageNo(query.getPageNo());
        result.setPageSize(query.getPageSize());
        result.setRecords(userExtensionMapper.selectPageByQuery(user, userInfo, query.getCreatedEndTime(),
                        (query.getPageNo() - 1) * query.getPageSize(), query.getPageSize()).stream()
                .map(this::toProfile)
                .toList());
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

    private UserEntity getActiveUser(String userId) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, userId)
                .eq(UserEntity::getStatus, 1));
        if (user == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        return user;
    }

    private void ensureIdentityAvailable(String identityType, String identityProvider, String identityValue) {
        UserIdentityEntity identity = userIdentityMapper.selectOne(new LambdaQueryWrapper<UserIdentityEntity>()
                .eq(UserIdentityEntity::getIdentityType, identityType)
                .eq(UserIdentityEntity::getIdentityProvider, identityProvider)
                .eq(UserIdentityEntity::getIdentityValue, identityValue));
        if (identity != null) {
            throw new BizException(ErrorCodeEnum.CREDENTIAL_ALREADY_IN_USE);
        }
    }

    private void updateIdentity(String userId, String identityType, String identityProvider, String identityValue) {
        UserIdentityEntity identity = userIdentityMapper.selectOne(new LambdaQueryWrapper<UserIdentityEntity>()
                .eq(UserIdentityEntity::getUserId, userId)
                .eq(UserIdentityEntity::getIdentityType, identityType)
                .eq(UserIdentityEntity::getIdentityProvider, identityProvider));
        if (identity == null) {
            identity = new UserIdentityEntity();
            identity.setUserId(userId);
            identity.setIdentityType(identityType);
            identity.setIdentityProvider(identityProvider);
            identity.setIdentityValue(identityValue);
            userIdentityMapper.insert(identity);
        } else {
            identity.setIdentityValue(identityValue);
            if (userIdentityMapper.updateById(identity) == 0) {
                throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
            }
        }
    }

    private UserProfileBO getProfile(String userId) {
        return userExtensionMapper.selectByUserIds(List.of(userId)).stream()
                .findFirst()
                .map(this::toProfile)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.USER_NOT_FOUND));
    }

    private void validateUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty() || userIds.stream().anyMatch(userId -> !StringUtils.hasText(userId))) {
            throw new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, ErrorCodeEnum.USER_ID_REQUIRED);
        }
    }

    private UserEntity toUserEntity(AdminUserQueryBO query) {
        UserEntity user = new UserEntity();
        user.setUserId(query.getUserId());
        user.setMobile(query.getMobile());
        user.setEmail(query.getEmail());
        user.setUsername(query.getUsername());
        user.setName(query.getName());
        user.setStatus(query.getStatus());
        user.setCtime(query.getCreatedStartTime());
        return user;
    }

    private UserInfoEntity toUserInfoEntity(AdminUserQueryBO query) {
        UserInfoEntity userInfo = new UserInfoEntity();
        userInfo.setAvatarUrl(query.getAvatarUrl());
        userInfo.setJobNumber(query.getJobNumber());
        return userInfo;
    }

    private UserWithTenantsBO toProfileWithTenants(UserProfileRow row, Map<String, List<UserTenantBO>> tenantsByUserId) {
        UserWithTenantsBO profile = new UserWithTenantsBO();
        copyProfile(toProfile(row), profile);
        profile.setTenants(tenantsByUserId.getOrDefault(row.getUserId(), List.of()));
        return profile;
    }

    private UserTenantBO toTenant(UserTenantRow row) {
        UserTenantBO tenant = new UserTenantBO();
        tenant.setTenantId(row.getTenantId());
        tenant.setTenantName(row.getTenantName());
        tenant.setTenantType(row.getTenantType());
        tenant.setTenantTypeDescription(TenantTypeEnum.descriptionOf(row.getTenantType()));
        return tenant;
    }

    private UserProfileBO toProfile(UserProfileRow row) {
        UserProfileBO profile = new UserProfileBO();
        profile.setUserId(row.getUserId());
        profile.setMobile(row.getMobile());
        profile.setEmail(row.getEmail());
        profile.setUsername(row.getUsername());
        profile.setName(row.getName());
        profile.setAvatarUrl(row.getAvatarUrl());
        profile.setJobNumber(row.getJobNumber());
        return profile;
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

