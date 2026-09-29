package com.ai.base.application.service.impl;

import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.service.UserService;
import com.ai.base.application.model.user.UserPageResult;
import com.ai.base.application.model.user.UserProfile;
import com.ai.base.application.model.user.UserQuery;
import com.ai.base.infrastructure.persistence.dto.UserProfileRow;
import com.ai.base.infrastructure.persistence.mapper.extension.UserExtensionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final UserExtensionMapper userExtensionMapper;

    public UserServiceImpl(UserExtensionMapper userExtensionMapper) {
        this.userExtensionMapper = userExtensionMapper;
    }

    @Override
    public UserProfile getProfile(UserQuery query) {
        if (!StringUtils.hasText(query.getUserId())) {
            throw new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, ErrorCodeEnum.USER_ID_REQUIRED);
        }
        List<UserProfileRow> rows = userExtensionMapper.selectPageByQuery(toPersistenceQuery(query));
        if (rows.isEmpty()) {
            throw new BizException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        return toProfile(rows.get(0));
    }

    @Override
    public UserPageResult page(UserQuery query) {
        com.ai.base.infrastructure.persistence.query.UserQuery persistenceQuery = toPersistenceQuery(query);
        UserPageResult result = new UserPageResult();
        result.setTotal(userExtensionMapper.countByQuery(persistenceQuery));
        result.setPageNo(query.getPageNo());
        result.setPageSize(query.getPageSize());
        if (result.getTotal() > 0) {
            result.setRecords(userExtensionMapper.selectPageByQuery(persistenceQuery).stream()
                    .map(this::toProfile)
                    .toList());
        } else {
            result.setRecords(List.of());
        }
        return result;
    }

    private com.ai.base.infrastructure.persistence.query.UserQuery toPersistenceQuery(UserQuery query) {
        com.ai.base.infrastructure.persistence.query.UserQuery persistenceQuery =
                new com.ai.base.infrastructure.persistence.query.UserQuery();
        persistenceQuery.setUserId(query.getUserId());
        persistenceQuery.setMobile(query.getMobile());
        persistenceQuery.setEmail(query.getEmail());
        persistenceQuery.setUsername(query.getUsername());
        persistenceQuery.setName(query.getName());
        persistenceQuery.setAvatarUrl(query.getAvatarUrl());
        persistenceQuery.setJobNumber(query.getJobNumber());
        persistenceQuery.setStatus(query.getStatus());
        persistenceQuery.setCreatedStartTime(query.getCreatedStartTime());
        persistenceQuery.setCreatedEndTime(query.getCreatedEndTime());
        persistenceQuery.setPageNo(query.getPageNo());
        persistenceQuery.setPageSize(query.getPageSize());
        persistenceQuery.setOffset((query.getPageNo() - 1) * query.getPageSize());
        return persistenceQuery;
    }

    private UserProfile toProfile(UserProfileRow row) {
        UserProfile profile = new UserProfile();
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

