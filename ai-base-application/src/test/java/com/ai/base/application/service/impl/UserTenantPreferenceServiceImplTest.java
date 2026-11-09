package com.ai.base.application.service.impl;

import com.ai.base.application.bo.UserTenantPreferenceBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;
import com.ai.base.application.port.UserPreferencePort;
import com.ai.base.application.service.TenantUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class UserTenantPreferenceServiceImplTest {
    private static final String USER_ID = "user-1";
    private static final LocalDateTime USED_AT = LocalDateTime.of(2026, 10, 27, 10, 0, 0, 123456000);
    private UserPreferencePort preferencePort;
    private TenantUserService tenantUserService;
    private UserTenantPreferenceServiceImpl service;

    @BeforeEach
    void setUp() {
        preferencePort = mock(UserPreferencePort.class);
        tenantUserService = mock(TenantUserService.class);
        service = new UserTenantPreferenceServiceImpl(preferencePort, tenantUserService);
        RequestContextHolder.set(new RequestContext("trace-1", "traceparent", USER_ID, "tenant-1", "session-1", "ai-base"));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void getByUserIdWithoutRecordReturnsNullAndDoesNotWrite() {
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(service.getByUserId(USER_ID)).isNull();
        verify(preferencePort).findByUserId(USER_ID);
        verifyNoMoreInteractions(preferencePort);
        verifyNoInteractions(tenantUserService);
    }

    @Test
    void getCurrentReturnsOnlyBusinessFields() {
        assertThat(UserTenantPreferenceBO.class.getDeclaredFields()).extracting("name")
                .containsExactly("userId", "defaultTenantId", "lastUsedTenantId");

        UserTenantPreferenceBO preference = preference();
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.of(preference));

        UserTenantPreferenceBO result = service.getCurrent();

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getDefaultTenantId()).isEqualTo("default-old");
        assertThat(result.getLastUsedTenantId()).isEqualTo("last-old");
        verify(preferencePort).findByUserId(USER_ID);
        verifyNoMoreInteractions(preferencePort);
    }

    @Test
    void defaultUpdateRequiresAdmissionAndUpdatesOnlyDefaultField() {
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.of(preference()));
        Mockito.when(preferencePort.updateDefaultTenantId(USER_ID, "default-new")).thenReturn(true);

        service.updateCurrentDefault("default-new");

        var order = inOrder(tenantUserService, preferencePort);
        order.verify(tenantUserService).requireAvailable(USER_ID, "default-new");
        order.verify(preferencePort).findByUserId(USER_ID);
        order.verify(preferencePort).updateDefaultTenantId(USER_ID, "default-new");
        verifyNoMoreInteractions(preferencePort, tenantUserService);
        verify(preferencePort, never()).updateLastUsed(anyString(), anyString(), Mockito.any(LocalDateTime.class));
        verify(preferencePort, never()).insertPreference(anyString(), anyString(), anyString(), Mockito.any(LocalDateTime.class));
    }

    @Test
    void nullDefaultClearsWithoutAdmission() {
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.of(preference()));
        Mockito.when(preferencePort.updateDefaultTenantId(USER_ID, null)).thenReturn(true);

        service.updateCurrentDefault(null);

        verify(preferencePort).updateDefaultTenantId(USER_ID, null);
        verifyNoInteractions(tenantUserService);
    }

    @Test
    void blankDefaultIsRejectedBeforeAdmissionOrPersistence() {
        for (String tenantId : new String[]{"", " ", "\t\n"}) {
            assertThatThrownBy(() -> service.updateCurrentDefault(tenantId))
                    .isInstanceOfSatisfying(BizException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.PARAMETER_FORMAT_INVALID));
        }
        verifyNoInteractions(preferencePort, tenantUserService);
    }

    @Test
    void admissionFailureDoesNotWrite() {
        BizException failure = new BizException(ErrorCodeEnum.PARAMETER_VALUE_INVALID);
        Mockito.when(tenantUserService.requireAvailable(USER_ID, "unavailable")).thenThrow(failure);

        assertThatThrownBy(() -> service.updateCurrentDefault("unavailable")).isSameAs(failure);

        verifyNoInteractions(preferencePort);
    }

    @Test
    void firstDefaultWriteInsertsOnlyRequiredBusinessFields() {
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.empty());
        Mockito.when(preferencePort.insertPreference(USER_ID, "default-new", null, null)).thenReturn(true);

        service.updateCurrentDefault("default-new");

        verify(preferencePort).insertPreference(USER_ID, "default-new", null, null);
        verify(preferencePort, never()).updateDefaultTenantId(anyString(), anyString());
        verify(tenantUserService).requireAvailable(USER_ID, "default-new");
    }

    @Test
    void firstNullDefaultWriteInsertsEmptyPreference() {
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.empty());
        Mockito.when(preferencePort.insertPreference(USER_ID, null, null, null)).thenReturn(true);

        service.updateCurrentDefault(null);

        verify(preferencePort).insertPreference(USER_ID, null, null, null);
        verifyNoInteractions(tenantUserService);
    }

    @Test
    void concurrentFirstDefaultInsertFallsBackToSingleFieldUpdate() {
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.empty());
        Mockito.when(preferencePort.insertPreference(USER_ID, "default-new", null, null)).thenReturn(false);
        Mockito.when(preferencePort.updateDefaultTenantId(USER_ID, "default-new")).thenReturn(true);

        service.updateCurrentDefault("default-new");

        verify(preferencePort).updateDefaultTenantId(USER_ID, "default-new");
        verify(preferencePort, never()).updateLastUsed(anyString(), anyString(), Mockito.any(LocalDateTime.class));
    }

    @Test
    void defaultUpdateWithZeroAffectedRowsThrowsConflict() {
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.of(preference()));

        assertThatThrownBy(() -> service.updateCurrentDefault(null))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.DATA_VERSION_CONFLICT));
    }

    @Test
    void defaultPersistenceFailurePropagates() {
        IllegalStateException failure = new IllegalStateException("database unavailable");
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.empty());
        Mockito.when(preferencePort.insertPreference(USER_ID, null, null, null)).thenThrow(failure);

        assertThatThrownBy(() -> service.updateCurrentDefault(null)).isSameAs(failure);
    }

    @Test
    void lastUsedUpdateChangesOnlyLastUsedFieldForExplicitUser() {
        Mockito.when(preferencePort.findByUserId("user-2")).thenReturn(Optional.of(preference()));
        Mockito.when(preferencePort.updateLastUsed("user-2", "last-new", USED_AT)).thenReturn(true);

        service.recordLastUsed("user-2", "last-new", USED_AT);

        verify(preferencePort).updateLastUsed("user-2", "last-new", USED_AT);
        verify(preferencePort, never()).insertPreference(anyString(), anyString(), anyString(), Mockito.any(LocalDateTime.class));
        verify(preferencePort, never()).updateDefaultTenantId(anyString(), anyString());
        verifyNoInteractions(tenantUserService);
    }

    @Test
    void firstLastUsedWriteInsertsOnlyRequiredBusinessFields() {
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.empty());
        Mockito.when(preferencePort.insertPreference(USER_ID, null, "last-new", USED_AT)).thenReturn(true);

        service.recordLastUsed(USER_ID, "last-new", USED_AT);

        verify(preferencePort).insertPreference(USER_ID, null, "last-new", USED_AT);
        verify(preferencePort, never()).updateLastUsed(anyString(), anyString(), Mockito.any(LocalDateTime.class));
        verifyNoInteractions(tenantUserService);
    }

    @Test
    void concurrentFirstLastUsedInsertFallsBackToSingleFieldUpdate() {
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.empty());
        Mockito.when(preferencePort.insertPreference(USER_ID, null, "last-new", USED_AT)).thenReturn(false);
        Mockito.when(preferencePort.updateLastUsed(USER_ID, "last-new", USED_AT)).thenReturn(true);

        service.recordLastUsed(USER_ID, "last-new", USED_AT);

        verify(preferencePort).updateLastUsed(USER_ID, "last-new", USED_AT);
        verify(preferencePort, never()).updateDefaultTenantId(anyString(), anyString());
    }

    @Test
    void lastUsedReadFailureDoesNotPropagate() {
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenThrow(new IllegalStateException("database unavailable"));
        assertThatCode(() -> service.recordLastUsed(USER_ID, "last-new", USED_AT)).doesNotThrowAnyException();
    }

    @Test
    void lastUsedInsertFailureDoesNotPropagate() {
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.empty());
        Mockito.when(preferencePort.insertPreference(USER_ID, null, "last-new", USED_AT))
                .thenThrow(new IllegalStateException("database unavailable"));
        assertThatCode(() -> service.recordLastUsed(USER_ID, "last-new", USED_AT)).doesNotThrowAnyException();
    }

    @Test
    void lastUsedUpdateFailureDoesNotPropagate() {
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.of(preference()));
        Mockito.when(preferencePort.updateLastUsed(USER_ID, "last-new", USED_AT))
                .thenThrow(new IllegalStateException("database unavailable"));
        assertThatCode(() -> service.recordLastUsed(USER_ID, "last-new", USED_AT)).doesNotThrowAnyException();
    }

    @Test
    void lastUsedForExistingPreferenceUpdateFailureDoesNotPropagate() {
        // findByUserId 抛错场景已由 lastUsedReadFailureDoesNotPropagate 覆盖，
        // 此处覆盖记录已存在但 updateLastUsed 抛错的容错路径
        Mockito.when(preferencePort.findByUserId(USER_ID)).thenReturn(Optional.of(preference()));
        IllegalStateException failure = new IllegalStateException("database unavailable");
        Mockito.when(preferencePort.updateLastUsed(USER_ID, "last-new", USED_AT)).thenThrow(failure);

        assertThatCode(() -> service.recordLastUsed(USER_ID, "last-new", USED_AT)).doesNotThrowAnyException();
        verify(preferencePort, never()).insertPreference(anyString(), anyString(), anyString(), Mockito.any(LocalDateTime.class));
    }

    private UserTenantPreferenceBO preference() {
        UserTenantPreferenceBO preference = new UserTenantPreferenceBO();
        preference.setUserId(USER_ID);
        preference.setDefaultTenantId("default-old");
        preference.setLastUsedTenantId("last-old");
        return preference;
    }
}
