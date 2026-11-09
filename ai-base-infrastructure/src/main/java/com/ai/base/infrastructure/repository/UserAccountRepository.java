package com.ai.base.infrastructure.repository;

import com.ai.base.application.bo.UserAccountBO;
import com.ai.base.application.bo.UserInfoBO;
import com.ai.base.application.bo.UserIdentityBO;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.common.BizException;
import com.ai.base.application.port.UserAccountPort;
import com.ai.base.infrastructure.persistence.entity.UserInfoEntity;
import com.ai.base.infrastructure.persistence.entity.UserEntity;
import com.ai.base.infrastructure.persistence.entity.UserIdentityEntity;
import com.ai.base.infrastructure.persistence.mapper.UserIdentityMapper;
import com.ai.base.infrastructure.persistence.mapper.UserInfoMapper;
import com.ai.base.infrastructure.persistence.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 用户账号存取端口实现：user / user_info / user_identity 三表访问。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Component
public class UserAccountRepository implements UserAccountPort {

    private final UserMapper userMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserIdentityMapper userIdentityMapper;

    public UserAccountRepository(UserMapper userMapper, UserInfoMapper userInfoMapper,
                                 UserIdentityMapper userIdentityMapper) {
        this.userMapper = userMapper;
        this.userInfoMapper = userInfoMapper;
        this.userIdentityMapper = userIdentityMapper;
    }

    @Override
    public Optional<UserAccountBO> findUser(String userId) {
        return Optional.ofNullable(userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUserId, userId)))
                .map(this::toAccount);
    }

    @Override
    public Optional<UserAccountBO> findActiveUser(String userId) {
        return Optional.ofNullable(userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUserId, userId)
                        .eq(UserEntity::getStatus, 1)))
                .map(this::toAccount);
    }

    @Override
    public boolean updateAccount(UserAccountBO account) {
        return userMapper.updateById(toEntity(account)) > 0;
    }

    @Override
    public Optional<UserInfoBO> findUserInfo(String userId) {
        return Optional.ofNullable(userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfoEntity>()
                        .eq(UserInfoEntity::getUserId, userId)))
                .map(this::toInfo);
    }

    @Override
    public void upsertUserInfo(UserInfoBO info) {
        UserInfoEntity existing = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfoEntity>()
                .eq(UserInfoEntity::getUserId, info.getUserId()));
        if (existing == null) {
            UserInfoEntity entity = new UserInfoEntity();
            entity.setUserId(info.getUserId());
            entity.setAvatarUrl(info.getAvatarUrl());
            entity.setJobNumber(info.getJobNumber());
            if (userInfoMapper.insert(entity) == 0) {
                throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
            }
            return;
        }
        existing.setAvatarUrl(info.getAvatarUrl());
        existing.setJobNumber(info.getJobNumber());
        if (userInfoMapper.updateById(existing) == 0) {
            throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
        }
    }

    @Override
    public Optional<UserIdentityBO> findIdentity(String identityType, String identityProvider, String identityValue) {
        return Optional.ofNullable(userIdentityMapper.selectOne(new LambdaQueryWrapper<UserIdentityEntity>()
                        .eq(UserIdentityEntity::getIdentityType, identityType)
                        .eq(UserIdentityEntity::getIdentityProvider, identityProvider)
                        .eq(UserIdentityEntity::getIdentityValue, identityValue)))
                .map(this::toIdentity);
    }

    @Override
    public Optional<UserIdentityBO> findUserIdentity(String userId, String identityType, String identityProvider) {
        return Optional.ofNullable(userIdentityMapper.selectOne(new LambdaQueryWrapper<UserIdentityEntity>()
                        .eq(UserIdentityEntity::getUserId, userId)
                        .eq(UserIdentityEntity::getIdentityType, identityType)
                        .eq(UserIdentityEntity::getIdentityProvider, identityProvider)))
                .map(this::toIdentity);
    }

    @Override
    public void upsertIdentity(String userId, String identityType, String identityProvider, String identityValue) {
        UserIdentityEntity existing = userIdentityMapper.selectOne(new LambdaQueryWrapper<UserIdentityEntity>()
                .eq(UserIdentityEntity::getUserId, userId)
                .eq(UserIdentityEntity::getIdentityType, identityType)
                .eq(UserIdentityEntity::getIdentityProvider, identityProvider));
        if (existing == null) {
            UserIdentityEntity entity = new UserIdentityEntity();
            entity.setUserId(userId);
            entity.setIdentityType(identityType);
            entity.setIdentityProvider(identityProvider);
            entity.setIdentityValue(identityValue);
            userIdentityMapper.insert(entity);
            return;
        }
        existing.setIdentityValue(identityValue);
        if (userIdentityMapper.updateById(existing) == 0) {
            throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
        }
    }

    private UserAccountBO toAccount(UserEntity entity) {
        UserAccountBO account = new UserAccountBO();
        account.setUserId(entity.getUserId());
        account.setMobile(entity.getMobile());
        account.setEmail(entity.getEmail());
        account.setUsername(entity.getUsername());
        account.setPasswordHash(entity.getPasswordHash());
        account.setName(entity.getName());
        account.setStatus(entity.getStatus());
        return account;
    }

    private UserEntity toEntity(UserAccountBO account) {
        UserEntity entity = new UserEntity();
        entity.setUserId(account.getUserId());
        entity.setMobile(account.getMobile());
        entity.setEmail(account.getEmail());
        entity.setUsername(account.getUsername());
        entity.setPasswordHash(account.getPasswordHash());
        entity.setName(account.getName());
        entity.setStatus(account.getStatus());
        return entity;
    }

    private UserInfoBO toInfo(UserInfoEntity entity) {
        UserInfoBO info = new UserInfoBO();
        info.setUserId(entity.getUserId());
        info.setAvatarUrl(entity.getAvatarUrl());
        info.setJobNumber(entity.getJobNumber());
        return info;
    }

    private UserIdentityBO toIdentity(UserIdentityEntity entity) {
        UserIdentityBO identity = new UserIdentityBO();
        identity.setUserId(entity.getUserId());
        identity.setIdentityType(entity.getIdentityType());
        identity.setIdentityProvider(entity.getIdentityProvider());
        identity.setIdentityValue(entity.getIdentityValue());
        return identity;
    }
}
