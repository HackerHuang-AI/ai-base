package com.ai.base.infrastructure.persistence.mapper.extension;

import com.ai.base.infrastructure.persistence.dto.UserProfileRow;
import com.ai.base.infrastructure.persistence.entity.UserEntity;
import com.ai.base.infrastructure.persistence.entity.UserInfoEntity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserExtensionMapper {
    long countByQuery(@Param("user") UserEntity user, @Param("userInfo") UserInfoEntity userInfo,
                      @Param("createdEndTime") LocalDateTime createdEndTime);

    List<UserProfileRow> selectPageByQuery(@Param("user") UserEntity user, @Param("userInfo") UserInfoEntity userInfo,
                                           @Param("createdEndTime") LocalDateTime createdEndTime,
                                           @Param("offset") long offset, @Param("pageSize") long pageSize);

    List<UserProfileRow> selectByUserIds(@Param("userIds") List<String> userIds);
}

