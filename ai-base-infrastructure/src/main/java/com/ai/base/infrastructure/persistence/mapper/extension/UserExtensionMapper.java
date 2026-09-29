package com.ai.base.infrastructure.persistence.mapper.extension;

import com.ai.base.infrastructure.persistence.dto.UserProfileRow;
import com.ai.base.infrastructure.persistence.query.UserQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserExtensionMapper {
    long countByQuery(@Param("query") UserQuery query);

    List<UserProfileRow> selectPageByQuery(@Param("query") UserQuery query);
}

