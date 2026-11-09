package com.ai.base.infrastructure.repository;

import com.ai.base.application.bo.AdminUserQueryBO;
import com.ai.base.application.bo.UserProfileBO;
import com.ai.base.application.port.UserProfileQueryPort;
import com.ai.base.infrastructure.persistence.dto.UserProfileRow;
import com.ai.base.infrastructure.persistence.entity.UserInfoEntity;
import com.ai.base.infrastructure.persistence.entity.UserEntity;
import com.ai.base.infrastructure.persistence.mapper.extension.UserExtensionMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户画像查询端口实现：扩展 SQL 关联查询。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Component
public class UserProfileQueryRepository implements UserProfileQueryPort {

    private final UserExtensionMapper userExtensionMapper;

    public UserProfileQueryRepository(UserExtensionMapper userExtensionMapper) {
        this.userExtensionMapper = userExtensionMapper;
    }

    @Override
    public List<UserProfileBO> findProfilesByUserIds(List<String> userIds) {
        return userExtensionMapper.selectByUserIds(userIds).stream()
                .map(this::toProfile)
                .toList();
    }

    @Override
    public long countUsers(AdminUserQueryBO query) {
        return userExtensionMapper.countByQuery(toUserEntity(query), toUserInfoEntity(query),
                query.getCreatedEndTime());
    }

    @Override
    public List<UserProfileBO> pageUsers(AdminUserQueryBO query, long offset, int pageSize) {
        return userExtensionMapper.selectPageByQuery(toUserEntity(query), toUserInfoEntity(query),
                        query.getCreatedEndTime(), offset, pageSize).stream()
                .map(this::toProfile)
                .toList();
    }

    private UserEntity toUserEntity(AdminUserQueryBO query) {
        UserEntity user = new UserEntity();
        user.setUserId(query.getUserId());
        user.setMobile(query.getMobile());
        user.setEmail(query.getEmail());
        user.setUsername(query.getUsername());
        user.setName(query.getName());
        user.setStatus(query.getStatus());
        user.setCreateTime(query.getCreatedStartTime());
        return user;
    }

    private UserInfoEntity toUserInfoEntity(AdminUserQueryBO query) {
        UserInfoEntity userInfo = new UserInfoEntity();
        userInfo.setAvatarUrl(query.getAvatarUrl());
        userInfo.setJobNumber(query.getJobNumber());
        return userInfo;
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
}
