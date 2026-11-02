package com.ai.base.starter.controller;

import com.ai.base.application.bo.UserTenantPreferenceBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.ResultCodeEnum;
import com.ai.base.application.service.UserTenantPreferenceService;
import com.ai.base.starter.vo.UserTenantPreferenceUpdateVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserTenantPreferenceControllerTest {
    private UserTenantPreferenceService service;
    private UserTenantPreferenceController controller;

    @BeforeEach
    void setUp() {
        service = mock(UserTenantPreferenceService.class);
        controller = new UserTenantPreferenceController(service);
    }

    @Test
    void getCurrentReturnsBusinessData() {
        UserTenantPreferenceBO preference = new UserTenantPreferenceBO();
        preference.setUserId("user-1");
        when(service.getCurrent()).thenReturn(preference);

        var result = controller.getCurrent();

        assertThat(result.getCode()).isEqualTo(ResultCodeEnum.SUCCESS);
        assertThat(result.getData()).isSameAs(preference);
    }

    @Test
    void getCurrentWithoutRecordReturnsSuccessWithNull() {
        var result = controller.getCurrent();
        assertThat(result.getCode()).isEqualTo(ResultCodeEnum.SUCCESS);
        assertThat(result.getData()).isNull();
    }

    @Test
    void updateDelegatesOnlyDefaultTenantId() {
        UserTenantPreferenceUpdateVO request = new UserTenantPreferenceUpdateVO();
        request.setDefaultTenantId("tenant-1");

        assertThat(controller.update(request).getCode()).isEqualTo(ResultCodeEnum.SUCCESS);

        verify(service).updateCurrentDefault("tenant-1");
        verifyNoMoreInteractions(service);
    }

    @Test
    void updatePassesNullForClearing() {
        controller.update(new UserTenantPreferenceUpdateVO());
        verify(service).updateCurrentDefault(null);
    }

    @Test
    void getCurrentPreservesBusinessException() {
        BizException failure = new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
        when(service.getCurrent()).thenThrow(failure);
        assertThatThrownBy(controller::getCurrent).isSameAs(failure);
    }

    @Test
    void updatePreservesBusinessException() {
        BizException failure = new BizException(ErrorCodeEnum.PARAMETER_FORMAT_INVALID);
        doThrow(failure).when(service).updateCurrentDefault(null);
        assertThatThrownBy(() -> controller.update(new UserTenantPreferenceUpdateVO())).isSameAs(failure);
    }

    @Test
    void getCurrentMapsUnexpectedExceptionToSystemError() {
        when(service.getCurrent()).thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(controller::getCurrent).isInstanceOfSatisfying(BizException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.SYSTEM_ERROR));
    }

    @Test
    void updateMapsUnexpectedExceptionToSystemError() {
        doThrow(new IllegalStateException("database unavailable")).when(service).updateCurrentDefault(null);
        assertThatThrownBy(() -> controller.update(new UserTenantPreferenceUpdateVO()))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.SYSTEM_ERROR));
    }
}

