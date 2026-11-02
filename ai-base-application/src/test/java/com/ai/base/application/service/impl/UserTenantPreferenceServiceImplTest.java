package com.ai.base.application.service.impl;

import com.ai.base.application.bo.UserTenantPreferenceBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.infrastructure.persistence.entity.UserTenantPreferenceEntity;
import com.ai.base.infrastructure.persistence.mapper.UserTenantPreferenceMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserTenantPreferenceServiceImplTest {
    private static final String USER_ID = "user-1";
    private static final LocalDateTime USED_AT = LocalDateTime.of(2026, 10, 27, 10, 0, 0, 123456000);
    private UserTenantPreferenceMapper mapper;
    private TenantUserService tenantUserService;
    private UserTenantPreferenceServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(UserTenantPreferenceMapper.class);
        tenantUserService = mock(TenantUserService.class);
        service = new UserTenantPreferenceServiceImpl(mapper, tenantUserService);
        RequestContextHolder.set(new RequestContext("trace-1", "traceparent", USER_ID, "tenant-1", "session-1", "ai-base"));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void getByUserIdWithoutRecordReturnsNullAndDoesNotWrite() {
        assertThat(service.getByUserId(USER_ID)).isNull();
        verify(mapper).selectOne(any());
        verifyNoMoreInteractions(mapper);
        verifyNoInteractions(tenantUserService);
    }

    @Test
    void getCurrentReturnsOnlyBusinessFields() {
        UserTenantPreferenceEntity entity = preference();
        entity.setId(123L);
        entity.setVersion(5);
        entity.setValid(1);
        when(mapper.selectOne(any())).thenReturn(entity);

        UserTenantPreferenceBO result = service.getCurrent();

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getDefaultTenantId()).isEqualTo("default-old");
        assertThat(result.getLastUsedTenantId()).isEqualTo("last-old");
        assertThat(UserTenantPreferenceBO.class.getDeclaredFields()).extracting("name")
                .containsExactly("userId", "defaultTenantId", "lastUsedTenantId");
        verify(mapper).selectOne(any());
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void defaultUpdateRequiresAdmissionAndUpdatesOnlyDefaultField() {
        UserTenantPreferenceEntity entity = preference();
        when(mapper.selectOne(any())).thenReturn(entity);
        when(mapper.updateDefaultTenantId(USER_ID, "default-new")).thenReturn(1);

        service.updateCurrentDefault("default-new");

        var order = inOrder(tenantUserService, mapper);
        order.verify(tenantUserService).requireAvailable(USER_ID, "default-new");
        order.verify(mapper).selectOne(any());
        order.verify(mapper).updateDefaultTenantId(USER_ID, "default-new");
        verifyNoMoreInteractions(mapper, tenantUserService);
        assertThat(entity.getLastUsedTenantId()).isEqualTo("last-old");
        assertThat(entity.getLastUsedAt()).isEqualTo(USED_AT.minusSeconds(1));
        assertThat(entity.getDefaultTenantId()).isEqualTo("default-old");
    }

    @Test
    void nullDefaultClearsWithoutAdmission() {
        when(mapper.selectOne(any())).thenReturn(preference());
        when(mapper.updateDefaultTenantId(USER_ID, null)).thenReturn(1);

        service.updateCurrentDefault(null);

        verify(mapper).updateDefaultTenantId(USER_ID, null);
        verifyNoInteractions(tenantUserService);
    }

    @Test
    void blankDefaultIsRejectedBeforeAdmissionOrPersistence() {
        for (String tenantId : new String[]{"", " ", "\t\n"}) {
            assertThatThrownBy(() -> service.updateCurrentDefault(tenantId))
                    .isInstanceOfSatisfying(BizException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.PARAMETER_FORMAT_INVALID));
        }
        verifyNoInteractions(mapper, tenantUserService);
    }

    @Test
    void admissionFailureDoesNotWrite() {
        BizException failure = new BizException(ErrorCodeEnum.PARAMETER_VALUE_INVALID);
        when(tenantUserService.requireAvailable(USER_ID, "unavailable")).thenThrow(failure);

        assertThatThrownBy(() -> service.updateCurrentDefault("unavailable")).isSameAs(failure);

        verifyNoInteractions(mapper);
    }

    @Test
    void firstDefaultWriteInsertsOnlyRequiredBusinessFields() {
        when(mapper.insert(any(UserTenantPreferenceEntity.class))).thenReturn(1);

        service.updateCurrentDefault("default-new");

        ArgumentCaptor<UserTenantPreferenceEntity> captor = ArgumentCaptor.forClass(UserTenantPreferenceEntity.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getDefaultTenantId()).isEqualTo("default-new");
        assertThat(captor.getValue().getLastUsedTenantId()).isNull();
        assertThat(captor.getValue().getLastUsedAt()).isNull();
        verify(mapper, never()).updateDefaultTenantId(anyString(), any());
    }

    @Test
    void firstNullDefaultWriteInsertsEmptyPreference() {
        when(mapper.insert(any(UserTenantPreferenceEntity.class))).thenReturn(1);

        service.updateCurrentDefault(null);

        verify(mapper).insert(org.mockito.ArgumentMatchers.<UserTenantPreferenceEntity>argThat(entity -> USER_ID.equals(entity.getUserId())
                && entity.getDefaultTenantId() == null && entity.getLastUsedTenantId() == null));
        verifyNoInteractions(tenantUserService);
    }

    @Test
    void concurrentFirstDefaultInsertFallsBackToSingleFieldUpdate() {
        when(mapper.insert(any(UserTenantPreferenceEntity.class))).thenThrow(new DuplicateKeyException("user_id"));
        when(mapper.updateDefaultTenantId(USER_ID, "default-new")).thenReturn(1);

        service.updateCurrentDefault("default-new");

        verify(mapper).updateDefaultTenantId(USER_ID, "default-new");
        verify(mapper, never()).updateLastUsedTenantId(anyString(), any(), any(LocalDateTime.class));
    }

    @Test
    void defaultUpdateWithZeroAffectedRowsThrowsConflict() {
        when(mapper.selectOne(any())).thenReturn(preference());

        assertThatThrownBy(() -> service.updateCurrentDefault(null))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.DATA_VERSION_CONFLICT));
    }

    @Test
    void defaultPersistenceFailurePropagates() {
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(mapper.insert(any(UserTenantPreferenceEntity.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.updateCurrentDefault(null)).isSameAs(failure);
    }

    @Test
    void lastUsedUpdateChangesOnlyLastUsedFieldForExplicitUser() {
        UserTenantPreferenceEntity entity = preference();
        when(mapper.selectOne(any())).thenReturn(entity);
        when(mapper.updateLastUsedTenantId("user-2", "last-new", USED_AT)).thenReturn(1);

        service.recordLastUsed("user-2", "last-new", USED_AT);

        verify(mapper).updateLastUsedTenantId("user-2", "last-new", USED_AT);
        verify(mapper, never()).insert(any(UserTenantPreferenceEntity.class));
        verify(mapper, never()).updateDefaultTenantId(anyString(), any());
        assertThat(entity.getDefaultTenantId()).isEqualTo("default-old");
        verifyNoInteractions(tenantUserService);
    }

    @Test
    void firstLastUsedWriteInsertsOnlyRequiredBusinessFields() {
        when(mapper.insert(any(UserTenantPreferenceEntity.class))).thenReturn(1);

        service.recordLastUsed(USER_ID, "last-new", USED_AT);

        verify(mapper).insert(org.mockito.ArgumentMatchers.<UserTenantPreferenceEntity>argThat(entity -> USER_ID.equals(entity.getUserId())
                && "last-new".equals(entity.getLastUsedTenantId()) && entity.getDefaultTenantId() == null
                && USED_AT.equals(entity.getLastUsedAt())));
        verify(mapper, never()).updateLastUsedTenantId(anyString(), any(), any(LocalDateTime.class));
    }

    @Test
    void concurrentFirstLastUsedInsertFallsBackToSingleFieldUpdate() {
        when(mapper.insert(any(UserTenantPreferenceEntity.class))).thenThrow(new DuplicateKeyException("user_id"));
        when(mapper.updateLastUsedTenantId(USER_ID, "last-new", USED_AT)).thenReturn(1);

        service.recordLastUsed(USER_ID, "last-new", USED_AT);

        verify(mapper).updateLastUsedTenantId(USER_ID, "last-new", USED_AT);
        verify(mapper, never()).updateDefaultTenantId(anyString(), any());
    }

    @Test
    void lastUsedReadFailureDoesNotPropagate() {
        when(mapper.selectOne(any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThatCode(() -> service.recordLastUsed(USER_ID, "last-new", USED_AT)).doesNotThrowAnyException();
    }

    @Test
    void lastUsedInsertFailureDoesNotPropagate() {
        when(mapper.insert(any(UserTenantPreferenceEntity.class))).thenThrow(new IllegalStateException("database unavailable"));
        assertThatCode(() -> service.recordLastUsed(USER_ID, "last-new", USED_AT)).doesNotThrowAnyException();
    }

    @Test
    void lastUsedUpdateFailureDoesNotPropagate() {
        when(mapper.selectOne(any())).thenReturn(preference());
        when(mapper.updateLastUsedTenantId(USER_ID, "last-new", USED_AT)).thenThrow(new IllegalStateException("database unavailable"));
        assertThatCode(() -> service.recordLastUsed(USER_ID, "last-new", USED_AT)).doesNotThrowAnyException();
    }

    @Test
    void olderAndEqualLastUsedEventsAreIgnoredWithoutFallbackWrites() {
        when(mapper.selectOne(any())).thenReturn(preference());
        for (LocalDateTime usedAt : new LocalDateTime[]{USED_AT.minusSeconds(2), USED_AT.minusSeconds(1)}) {
            assertThatCode(() -> service.recordLastUsed(USER_ID, "last-new", usedAt)).doesNotThrowAnyException();
            verify(mapper).updateLastUsedTenantId(USER_ID, "last-new", usedAt);
        }
        verify(mapper, never()).insert(any(UserTenantPreferenceEntity.class));
        verify(mapper, never()).updateDefaultTenantId(anyString(), any());
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void concurrentFirstLastUsedInsertWithOlderEventIsIgnored() {
        when(mapper.insert(any(UserTenantPreferenceEntity.class))).thenThrow(new DuplicateKeyException("user_id"));

        assertThatCode(() -> service.recordLastUsed(USER_ID, "last-new", USED_AT)).doesNotThrowAnyException();

        verify(mapper).updateLastUsedTenantId(USER_ID, "last-new", USED_AT);
        verify(mapper, never()).updateDefaultTenantId(anyString(), any());
    }

    private UserTenantPreferenceEntity preference() {
        UserTenantPreferenceEntity entity = new UserTenantPreferenceEntity();
        entity.setUserId(USER_ID);
        entity.setDefaultTenantId("default-old");
        entity.setLastUsedTenantId("last-old");
        entity.setLastUsedAt(USED_AT.minusSeconds(1));
        return entity;
    }
}

